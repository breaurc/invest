# valuation-app

Spring Boot port of `Exemplu de calcul.xlsx`: base-year facts come from Financial Modeling Prep,
the user enters growth / PE assumptions, the app computes the projected EPS and share-price range.

## Run

1. Get a free API key at https://site.financialmodelingprep.com/ (Developer -> Dashboard).
2. Set it as an environment variable and start the app:

   ```powershell
   $env:FMP_API_KEY = "your_key_here"
   mvn spring-boot:run
   ```
3. Open http://localhost:8080, type a ticker (e.g. `AAPL`, `MSFT`), click **Load data**.

## Local file store (cache + your guidance)

One JSON file per ticker under `./data` (override with `APP_DATA_DIR`), e.g. `data/AAPL.json`:

| Section      | Content                                             | Refresh policy                               |
|--------------|-----------------------------------------------------|----------------------------------------------|
| `financials` | last annual statement facts + historical growth     | reused for `app.financials-ttl` (7 days)     |
| `estimates`  | analyst consensus per year + price-target range     | reused for `app.estimates-ttl` (1 day)       |
| `guidance`   | company guidance you typed in the GUIDANCE rows     | never fetched; saved on every edit           |

The quote (price, 52-week range) is always fetched live. **Refresh data** in the UI (or `?refresh=true` on the API)
bypasses the TTLs. If FMP fails and a cached copy exists, the cached copy is served.

## Mapping to the spreadsheet

| Sheet cell            | Source                                   |
|-----------------------|------------------------------------------|
| Revenue (DATA)        | FMP `/income-statement` -> `revenue`     |
| EPS (DATA)            | `epsDiluted` (falls back to `eps`)       |
| No. Shares            | `weightedAverageShsOutDil`               |
| Net Income (base)     | EPS * No. Shares                         |
| FY end / filing date  | `date` / `filingDate` of the statement   |
| Revenue / Net Inc. growth (base year) | base vs. previous year's statement |
| Share Price Low/High  | FMP `/quote` -> `yearLow` / `yearHigh`   |
| PE Low/High (DATA)    | 52w low / EPS, 52w high / EPS            |
| Revenue Growth, Net Inc. Growth, PE Low/High Est | user INPUT     |
| Everything else       | computed in `ValuationService`           |

## API

- `GET  /api/base/{symbol}`  -> base-year DATA
- `GET  /api/base/{symbol}?refresh=true` -> same, ignoring the cache
- `GET|PUT /api/guidance/{symbol}` -> `{ "2027": {revenueGrowthPct, netIncomeGrowthPct}, ... }` manual company guidance
- `GET  /api/estimates/{symbol}?baseYear=2026[&refresh=true]` -> `{ years:[analyst consensus revenue / net income / EPS low-avg-high per later fiscal year], priceTarget:{low, high, consensus, median} }`
- `POST /api/projection`      -> `{ base, years:[{revenueGrowthPct, netIncomeGrowthPct, peLow, peHigh}, ...] }`
