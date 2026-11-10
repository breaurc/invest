package ro.valuation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.valuation.AppProperties;
import ro.valuation.fmp.FmpClient;
import ro.valuation.model.AnalystEstimate;
import ro.valuation.model.BaseData;
import ro.valuation.model.EstimatesResponse;
import ro.valuation.model.FinancialsSnapshot;
import ro.valuation.model.GuidanceYear;
import ro.valuation.model.PriceTargetConsensus;
import ro.valuation.model.QuoteSnapshot;
import ro.valuation.model.TickerRecord;
import ro.valuation.model.TickerRecord.StoredEstimates;
import ro.valuation.store.TickerStore;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Decides what comes from the local file store and what is fetched live:
 *  - statement facts (FinancialsSnapshot): reused for {@code app.financials-ttl}, then refreshed
 *  - analyst consensus + price targets:    reused for {@code app.estimates-ttl}, then refreshed
 *  - quote (price, 52w range):             always live
 *  - user guidance:                        stored only, never fetched
 * {@code refresh=true} bypasses the TTL. If FMP fails and a cached copy exists, the cached copy is served.
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final FmpClient fmp;
    private final TickerStore store;
    private final AppProperties props;

    public MarketDataService(FmpClient fmp, TickerStore store, AppProperties props) {
        this.fmp = fmp;
        this.store = store;
        this.props = props;
    }

    public BaseData base(String symbol, boolean refresh) {
        String sym = norm(symbol);
        TickerRecord rec = store.load(sym);
        FinancialsSnapshot fin = rec.financials();
        boolean cached = fin != null && !refresh && fresh(fin.fetchedAt(), props.financialsTtl());
        if (!cached) {
            try {
                fin = fmp.fetchFinancials(sym);
                store.save(rec.withFinancials(fin));
            } catch (ResponseStatusException e) {
                if (fin == null) throw e;
                log.warn("FMP financials failed for {} ({}); serving cached copy from {}", sym, e.getReason(), fin.fetchedAt());
                cached = true;
            }
        }
        QuoteSnapshot q = fmp.fetchQuote(sym);   // live, every time
        double eps = fin.eps();
        return new BaseData(
                sym, q.name(), fin.fiscalYear(), fin.periodEndDate(), fin.filingDate(),
                fin.revenue(), fin.netIncome(), eps, fin.sharesOutstanding(),
                fin.revenueGrowthPct(), fin.netIncomeGrowthPct(),
                q.price(), q.yearLow(), q.yearHigh(),
                eps != 0 ? q.yearLow() / eps : 0,
                eps != 0 ? q.yearHigh() / eps : 0,
                eps != 0 ? q.price() / eps : 0,
                fin.fetchedAt().toString(), cached);
    }

    public EstimatesResponse estimates(String symbol, int baseFiscalYear, boolean refresh) {
        String sym = norm(symbol);
        TickerRecord rec = store.load(sym);
        StoredEstimates est = rec.estimates();
        boolean cached = est != null && !refresh && fresh(est.fetchedAt(), props.estimatesTtl());
        if (!cached) {
            List<AnalystEstimate> years = fmp.fetchEstimates(sym);
            PriceTargetConsensus pt = fmp.fetchPriceTarget(sym);
            if (years.isEmpty() && pt == null && est != null) {
                log.warn("FMP consensus empty for {}; serving cached copy from {}", sym, est.fetchedAt());
                cached = true;
            } else {
                est = new StoredEstimates(years, pt, Instant.now());
                store.save(rec.withEstimates(est));
            }
        }
        List<AnalystEstimate> future = est.years().stream().filter(e -> e.fiscalYear() > baseFiscalYear).toList();
        return new EstimatesResponse(future, est.priceTarget(), est.fetchedAt().toString(), cached);
    }

    public Map<Integer, GuidanceYear> guidance(String symbol) {
        return new TreeMap<>(store.load(norm(symbol)).guidance());
    }

    public Map<Integer, GuidanceYear> saveGuidance(String symbol, Map<Integer, GuidanceYear> guidance) {
        String sym = norm(symbol);
        Map<Integer, GuidanceYear> clean = new TreeMap<>();
        guidance.forEach((year, g) -> {
            if (g != null && (g.revenueGrowthPct() != null || g.netIncomeGrowthPct() != null)) clean.put(year, g);
        });
        store.save(store.load(sym).withGuidance(clean));
        return clean;
    }

    private static boolean fresh(Instant fetchedAt, Duration ttl) {
        return fetchedAt != null && fetchedAt.plus(ttl).isAfter(Instant.now());
    }

    private static String norm(String symbol) { return symbol.trim().toUpperCase(); }
}
