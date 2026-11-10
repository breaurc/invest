package ro.valuation.fmp;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import ro.valuation.model.AnalystEstimate;
import ro.valuation.model.FinancialsSnapshot;
import ro.valuation.model.PriceTargetConsensus;
import ro.valuation.model.QuoteSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Thin client over the Financial Modeling Prep "stable" API. Pure fetches, no caching:
 *  - /income-statement        -> last two annual statements (facts + historical growth)   [cached by MarketDataService]
 *  - /quote                   -> current price, 52-week low / high                        [always live]
 *  - /analyst-estimates       -> consensus revenue / net income / EPS per fiscal year     [cached]
 *  - /price-target-consensus  -> analyst price-target low / high / consensus / median     [cached]
 */
@Component
public class FmpClient {

    private static final Logger log = LoggerFactory.getLogger(FmpClient.class);

    private final RestClient client;
    private final FmpProperties props;

    public FmpClient(FmpProperties props) {
        this.props = props;
        this.client = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    public FinancialsSnapshot fetchFinancials(String symbol) {
        requireKey();
        String sym = norm(symbol);
        JsonNode incomeArr = get("/income-statement", sym, "&period=annual&limit=2");
        if (incomeArr == null || !incomeArr.isArray() || incomeArr.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No income statement found for " + sym);
        }
        JsonNode is = incomeArr.get(0);

        int fiscalYear = is.path("fiscalYear").asInt(0);
        String periodEndDate = is.path("date").asText(null);
        if (fiscalYear == 0 && periodEndDate != null && periodEndDate.length() >= 4) {
            fiscalYear = Integer.parseInt(periodEndDate.substring(0, 4));
        }
        String filingDate = is.path("filingDate").asText(null);

        double revenue = is.path("revenue").asDouble();
        double netIncome = is.path("netIncome").asDouble();
        double eps = firstNonZero(is.path("epsDiluted").asDouble(), is.path("eps").asDouble());
        double shares = firstNonZero(is.path("weightedAverageShsOutDil").asDouble(),
                                     is.path("weightedAverageShsOut").asDouble());
        if (shares == 0 && eps != 0) shares = netIncome / eps;

        // Historical growth of the base year vs. the previous fiscal year (FMP returns newest first)
        Double revenueGrowthPct = null, netIncomeGrowthPct = null;
        if (incomeArr.size() > 1) {
            JsonNode prev = incomeArr.get(1);
            revenueGrowthPct = growthPct(prev.path("revenue").asDouble(), revenue);
            netIncomeGrowthPct = growthPct(prev.path("netIncome").asDouble(), netIncome);
        }
        return new FinancialsSnapshot(fiscalYear, periodEndDate, filingDate, revenue, netIncome, eps, shares,
                revenueGrowthPct, netIncomeGrowthPct, Instant.now());
    }

    public QuoteSnapshot fetchQuote(String symbol) {
        requireKey();
        String sym = norm(symbol);
        JsonNode quoteArr = get("/quote", sym, "");
        if (quoteArr == null || !quoteArr.isArray() || quoteArr.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No quote found for " + sym);
        }
        JsonNode q = quoteArr.get(0);
        return new QuoteSnapshot(q.path("name").asText(sym), q.path("price").asDouble(),
                q.path("yearLow").asDouble(), q.path("yearHigh").asDouble());
    }

    /** All annual analyst estimates FMP has, oldest first. Empty when the plan / data does not provide them. */
    public List<AnalystEstimate> fetchEstimates(String symbol) {
        String sym = norm(symbol);
        JsonNode arr;
        try {
            arr = get("/analyst-estimates", sym, "&period=annual&limit=10");
        } catch (ResponseStatusException e) {
            log.warn("Analyst estimates unavailable for {}: {}", sym, e.getReason());
            return List.of();
        }
        List<AnalystEstimate> out = new ArrayList<>();
        if (arr == null || !arr.isArray()) return out;
        for (JsonNode n : arr) {
            String date = n.path("date").asText("");
            if (date.length() < 4) continue;
            out.add(new AnalystEstimate(
                    Integer.parseInt(date.substring(0, 4)), date,
                    n.path("revenueLow").asDouble(), n.path("revenueAvg").asDouble(), n.path("revenueHigh").asDouble(),
                    n.path("netIncomeLow").asDouble(), n.path("netIncomeAvg").asDouble(), n.path("netIncomeHigh").asDouble(),
                    n.path("epsLow").asDouble(), n.path("epsAvg").asDouble(), n.path("epsHigh").asDouble(),
                    n.path("numAnalystsRevenue").asInt(), n.path("numAnalystsEps").asInt()));
        }
        out.sort(Comparator.comparingInt(AnalystEstimate::fiscalYear));
        return out;
    }

    /** Analyst price-target range, or null when the plan / data does not provide it. */
    public PriceTargetConsensus fetchPriceTarget(String symbol) {
        String sym = norm(symbol);
        try {
            JsonNode arr = get("/price-target-consensus", sym, "");
            if (arr == null || !arr.isArray() || arr.isEmpty()) return null;
            JsonNode n = arr.get(0);
            double low = n.path("targetLow").asDouble(), high = n.path("targetHigh").asDouble();
            if (low == 0 && high == 0) return null;
            return new PriceTargetConsensus(low, high,
                    n.path("targetConsensus").asDouble(), n.path("targetMedian").asDouble());
        } catch (ResponseStatusException e) {
            log.warn("Price target consensus unavailable for {}: {}", sym, e.getReason());
            return null;
        }
    }

    private void requireKey() {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "FMP_API_KEY is not set. Get a free key at financialmodelingprep.com and export it as FMP_API_KEY.");
        }
    }

    private static String norm(String symbol) { return symbol.trim().toUpperCase(); }

    private JsonNode get(String path, String symbol, String extra) {
        try {
            return client.get()
                    .uri(path + "?symbol={s}" + extra + "&apikey={k}", symbol, props.apiKey())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "FMP free plan does not cover '" + symbol + "' (non-US exchanges need a paid plan). "
                        + "Try a US-listed ticker such as AAPL or MSFT. FMP said: " + e.getResponseBodyAsString());
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "FMP " + path + " returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        }
    }

    private static double firstNonZero(double a, double b) { return a != 0 ? a : b; }

    /** Percentage change from {@code prev} to {@code curr}; null when the previous value is 0 (undefined). */
    private static Double growthPct(double prev, double curr) {
        return prev != 0 ? (curr / prev - 1) * 100 : null;
    }
}
