package ro.valuation.model;

/** Analyst consensus for one future fiscal year (FMP /analyst-estimates, annual). */
public record AnalystEstimate(
        int fiscalYear,             // year of the fiscal period end date
        String periodEndDate,
        double revenueLow,
        double revenueAvg,
        double revenueHigh,
        double netIncomeLow,
        double netIncomeAvg,
        double netIncomeHigh,
        double epsLow,
        double epsAvg,
        double epsHigh,
        int numAnalystsRevenue,
        int numAnalystsEps
) {}
