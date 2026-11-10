package ro.valuation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Local file store settings.
 *  dataDir       - one JSON file per ticker (cached FMP data + user guidance)
 *  financialsTtl - how long the last annual statement facts are reused before asking FMP again
 *  estimatesTtl  - how long analyst estimates / price targets are reused
 * Quotes (price, 52w range) are never cached.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String dataDir, Duration financialsTtl, Duration estimatesTtl) {
}
