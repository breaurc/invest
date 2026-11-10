package ro.valuation.model;

import java.time.Instant;

/** Facts from the last annual income statement. Changes once a year, so it is cached on disk. */
public record FinancialsSnapshot(
        int fiscalYear,
        String periodEndDate,
        String filingDate,
        double revenue,
        double netIncome,
        double eps,
        double sharesOutstanding,
        Double revenueGrowthPct,
        Double netIncomeGrowthPct,
        Instant fetchedAt
) {}
