package ro.valuation.model;

import java.util.List;

/** Everything the UI needs for the CONSENSUS rows. {@code priceTarget} is null when unavailable. */
public record EstimatesResponse(
        List<AnalystEstimate> years,
        PriceTargetConsensus priceTarget,
        String fetchedAt,   // when the consensus was last fetched from FMP (ISO instant)
        boolean cached      // true when served from the local file store
) {}
