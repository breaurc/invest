package ro.valuation.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.valuation.model.BaseData;
import ro.valuation.model.EstimatesResponse;
import ro.valuation.model.GuidanceYear;
import ro.valuation.model.ProjectionRequest;
import ro.valuation.model.YearResult;
import ro.valuation.service.MarketDataService;
import ro.valuation.service.ValuationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ValuationController {

    private final MarketDataService market;
    private final ValuationService service;

    public ValuationController(MarketDataService market, ValuationService service) {
        this.market = market;
        this.service = service;
    }

    /** DATA column: statement facts from the file store (or FMP when stale / refresh=true) + live quote. */
    @GetMapping("/base/{symbol}")
    public BaseData base(@PathVariable String symbol, @RequestParam(defaultValue = "false") boolean refresh) {
        return market.base(symbol, refresh);
    }

    /** Analyst consensus (per-year estimates + price-target range) for the fiscal years after {@code baseYear}. */
    @GetMapping("/estimates/{symbol}")
    public EstimatesResponse estimates(@PathVariable String symbol, @RequestParam int baseYear,
                                       @RequestParam(defaultValue = "false") boolean refresh) {
        return market.estimates(symbol, baseYear, refresh);
    }

    /** Manual company guidance per fiscal year, stored in the ticker's file. */
    @GetMapping("/guidance/{symbol}")
    public Map<Integer, GuidanceYear> guidance(@PathVariable String symbol) {
        return market.guidance(symbol);
    }

    @PutMapping("/guidance/{symbol}")
    public Map<Integer, GuidanceYear> saveGuidance(@PathVariable String symbol,
                                                   @RequestBody Map<Integer, GuidanceYear> guidance) {
        return market.saveGuidance(symbol, guidance);
    }

    /** INPUT -> COMPUTED: runs the spreadsheet logic for the given assumptions. */
    @PostMapping("/projection")
    public List<YearResult> projection(@Valid @RequestBody ProjectionRequest req) {
        return service.project(req);
    }
}
