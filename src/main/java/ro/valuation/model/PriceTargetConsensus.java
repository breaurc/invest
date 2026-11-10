package ro.valuation.model;

/** Analyst price-target range (FMP /price-target-consensus); targets are typically 12-month targets. */
public record PriceTargetConsensus(
        double targetLow,
        double targetHigh,
        double targetConsensus,
        double targetMedian
) {}
