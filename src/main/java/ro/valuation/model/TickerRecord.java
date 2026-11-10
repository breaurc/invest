package ro.valuation.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Everything stored on disk for one ticker: data/<SYMBOL>.json */
public record TickerRecord(
        String symbol,
        FinancialsSnapshot financials,
        StoredEstimates estimates,
        Map<Integer, GuidanceYear> guidance
) {
    public record StoredEstimates(List<AnalystEstimate> years, PriceTargetConsensus priceTarget, Instant fetchedAt) {}

    public static TickerRecord empty(String symbol) {
        return new TickerRecord(symbol, null, null, Map.of());
    }
    public TickerRecord withFinancials(FinancialsSnapshot f) { return new TickerRecord(symbol, f, estimates, guidance); }
    public TickerRecord withEstimates(StoredEstimates e)     { return new TickerRecord(symbol, financials, e, guidance); }
    public TickerRecord withGuidance(Map<Integer, GuidanceYear> g) { return new TickerRecord(symbol, financials, estimates, g); }
}
