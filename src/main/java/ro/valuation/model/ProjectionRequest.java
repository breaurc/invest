package ro.valuation.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** INPUT: base-year facts plus one set of assumptions per projected year. */
public record ProjectionRequest(
        @NotNull @Valid BaseData base,
        @NotNull @Valid List<YearInput> years
) {
    public record YearInput(
            double revenueGrowthPct,
            double netIncomeGrowthPct,
            double peLow,
            double peHigh
    ) {}
}
