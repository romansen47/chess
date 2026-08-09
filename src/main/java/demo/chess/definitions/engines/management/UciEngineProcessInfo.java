package demo.chess.definitions.engines.management;

public record UciEngineProcessInfo(
        String id,
        String label,
        String engineType,
        String enginePath,
        Long pid,
        boolean processAlive,
        String state,
        String createdAt,
        String processStartedAt,
        String lastActivityAt,
        Integer exitCode,
        long logEntryCount) {
}
