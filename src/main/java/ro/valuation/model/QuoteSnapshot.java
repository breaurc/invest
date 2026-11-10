package ro.valuation.model;

/** Live market data: fetched on every request, never cached. */
public record QuoteSnapshot(String name, double price, double yearLow, double yearHigh) {}
