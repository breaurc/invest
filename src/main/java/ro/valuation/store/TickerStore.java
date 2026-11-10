package ro.valuation.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ro.valuation.AppProperties;
import ro.valuation.model.TickerRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Minimal file database: one pretty-printed JSON document per ticker under {@code app.data-dir}.
 * Holds the slow-moving FMP data (statement facts, analyst consensus) and the user's manual guidance.
 */
@Component
public class TickerStore {

    private static final Logger log = LoggerFactory.getLogger(TickerStore.class);

    private final ObjectMapper mapper;
    private final Path dir;

    public TickerStore(ObjectMapper mapper, AppProperties props) {
        this.mapper = mapper;
        this.dir = Path.of(props.dataDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create data dir " + dir, e);
        }
        log.info("Ticker file store at {}", dir);
    }

    public synchronized TickerRecord load(String symbol) {
        Path f = file(symbol);
        if (!Files.exists(f)) return TickerRecord.empty(symbol);
        try {
            return mapper.readValue(f.toFile(), TickerRecord.class);
        } catch (IOException e) {
            log.warn("Unreadable record {} ({}); starting fresh", f, e.getMessage());
            return TickerRecord.empty(symbol);
        }
    }

    public synchronized void save(TickerRecord record) {
        Path f = file(record.symbol());
        try {
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), record);
            Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + f, e);
        }
    }

    public Path dir() { return dir; }

    private Path file(String symbol) {
        String safe = symbol.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9.-]", "_");
        return dir.resolve(safe + ".json");
    }
}
