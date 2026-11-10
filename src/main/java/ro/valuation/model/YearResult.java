package ro.valuation.model;

/** One column of the model (base year or projected year). */
public record YearResult(
        int year,
        boolean base,
        double revenue,
        Double revenueGrowthPct,
        double netIncome,
        Double netIncomeGrowthPct,
        double netIncomeMarginPct,
        double eps,
        double peLow,
        double peHigh,
        double sharePriceLow,
        double sharePriceHigh
) {}
