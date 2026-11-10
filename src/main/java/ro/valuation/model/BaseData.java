package ro.valuation.model;

/** The DATA column of the model: facts for the base (last reported) fiscal year + live quote. */
public record BaseData(
        String symbol,
        String name,
        int fiscalYear,
        String periodEndDate,       // last day of the base fiscal year (FMP "date")
        String filingDate,          // date the annual statement was filed / issued (FMP "filingDate")
        double revenue,
        double netIncome,
        double eps,
        double sharesOutstanding,
        Double revenueGrowthPct,    // base year vs. prior year, in %; null if prior year unavailable
        Double netIncomeGrowthPct,  // base year vs. prior year, in %; null if prior year unavailable
        double price,
        double priceLow52w,
        double priceHigh52w,
        double peLow,     // 52w low  / EPS
        double peHigh,    // 52w high / EPS
        double peCurrent, // price    / EPS
        String financialsFetchedAt, // when the statement facts were last fetched from FMP (ISO instant)
        boolean financialsCached    // true when the statement facts came from the local file store
) {}
