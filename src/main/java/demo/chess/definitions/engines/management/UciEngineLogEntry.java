package demo.chess.definitions.engines.management;

public record UciEngineLogEntry(
        long sequence,
        String timestamp,
        String direction,
        String message) {
}
