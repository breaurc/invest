package ro.valuation.service;

import org.springframework.stereotype.Service;
import ro.valuation.model.BaseData;
import ro.valuation.model.ProjectionRequest;
import ro.valuation.model.YearResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure implementation of the spreadsheet logic.
 *
 *  Base year (DATA):
 *    Net Income        = EPS * No.Shares
 *    Net Income Margin = Net Income / Revenue * 100
 *    Share Price Low   = 52w low,  Share Price High = 52w high
 *    PE Low/High       = price low/high / EPS
 *
 *  Projected year N (INPUT: growth %, PE low, PE high):
 *    Revenue           = Revenue(N-1)    * (1 + revenueGrowth)
 *    Net Income        = Net Income(N-1) * (1 + netIncomeGrowth)
 *    Net Income Margin = Net Income / Revenue * 100
 *    EPS               = Net Income / No.Shares      (shares assumed constant)
 *    Share Price Low   = EPS * PE Low
 *    Share Price High  = EPS * PE High
 */
@Service
public class ValuationService {

    public List<YearResult> project(ProjectionRequest req) {
        BaseData b = req.base();
        List<YearResult> out = new ArrayList<>();

        double shares = b.sharesOutstanding();
        double baseNetIncome = b.eps() * shares;          // as in the sheet: EPS * No.Shares
        double prevRevenue = b.revenue();
        double prevNetIncome = baseNetIncome;

        out.add(new YearResult(
                b.fiscalYear(), true,
                b.revenue(), null,
                baseNetIncome, null,
                margin(baseNetIncome, b.revenue()),
                b.eps(),
                b.peLow(), b.peHigh(),
                b.priceLow52w(), b.priceHigh52w()));

        int year = b.fiscalYear();
        for (ProjectionRequest.YearInput in : req.years()) {
            year++;
            double revenue = prevRevenue * (1 + in.revenueGrowthPct() / 100.0);
            double netIncome = prevNetIncome * (1 + in.netIncomeGrowthPct() / 100.0);
            double eps = shares != 0 ? netIncome / shares : 0;

            out.add(new YearResult(
                    year, false,
                    revenue, in.revenueGrowthPct(),
                    netIncome, in.netIncomeGrowthPct(),
                    margin(netIncome, revenue),
                    eps,
                    in.peLow(), in.peHigh(),
                    eps * in.peLow(), eps * in.peHigh()));

            prevRevenue = revenue;
            prevNetIncome = netIncome;
        }
        return out;
    }

    private static double margin(double netIncome, double revenue) {
        return revenue != 0 ? netIncome / revenue * 100.0 : 0;
    }
}
