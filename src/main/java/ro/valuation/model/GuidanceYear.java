package ro.valuation.model;

/** Company guidance for one fiscal year, entered manually by the user (from the earnings release / 8-K). */
public record GuidanceYear(Double revenueGrowthPct, Double netIncomeGrowthPct) {}
