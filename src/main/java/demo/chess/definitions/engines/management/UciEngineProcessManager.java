package demo.chess.definitions.engines.management;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight in-process registry for UCI engine instances.
 *
 * It intentionally does not own the chess-engine lifecycle. Existing engine classes keep
 * ownership; this registry only observes the current Process, records UCI traffic and offers
 * an emergency terminate operation for diagnostics.
 */
public final class UciEngineProcessManager {

    private static final int MAX_LOG_ENTRIES = 2000;
    private static final Map<String, ManagedEngine> ENGINES = new ConcurrentHashMap<>();

    private UciEngineProcessManager() {
    }

    public static String register(String engineType, String enginePath) {
        String id = UUID.randomUUID().toString();
        ManagedEngine engine = new ManagedEngine(id, engineType, enginePath);
        ENGINES.put(id, engine);
        engine.addLog("SYSTEM", "Engine instance registered");
        return id;
    }

    public static void setLabel(String id, String label) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine != null && label != null && !label.isBlank()) {
            engine.label = label.trim();
            engine.touch();
        }
    }

    public static void attachProcess(String id, Process process) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine == null) {
            return;
        }
        engine.process = process;
        engine.processStartedAt = Instant.now();
        engine.exitCode = null;
        engine.closed = false;
        engine.addLog("SYSTEM", "Process started with PID " + safePid(process));
    }

    public static void processEnded(String id, Process process) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine == null || engine.process != process) {
            return;
        }
        engine.exitCode = exitCode(process);
        engine.addLog("SYSTEM", "Process ended" + formatExitCode(engine.exitCode));
    }

    public static void markClosed(String id) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine != null) {
            engine.closed = true;
            engine.addLog("SYSTEM", "Engine instance closed");
        }
    }

    public static void logCommand(String id, String commandBlock) {
        logBlock(id, "COMMAND", commandBlock);
    }

    public static void logResponse(String id, String response) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine != null && response != null) {
            engine.addLog("RESPONSE", response);
        }
    }

    public static List<UciEngineProcessInfo> list() {
        return ENGINES.values().stream()
                .sorted(Comparator.comparing((ManagedEngine engine) -> engine.createdAt).reversed())
                .map(UciEngineProcessManager::toInfo)
                .toList();
    }

    public static List<UciEngineLogEntry> log(String id) {
        ManagedEngine engine = ENGINES.get(id);
        return engine == null ? List.of() : engine.logSnapshot();
    }

    public static boolean terminate(String id) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine == null) {
            return false;
        }

        Process process = engine.process;
        if (process == null || !process.isAlive()) {
            engine.addLog("SYSTEM", "Terminate requested, but no live process exists");
            return true;
        }

        engine.addLog("SYSTEM", "Terminate requested for PID " + safePid(process));
        process.destroy();
        try {
            if (!process.waitFor(750, TimeUnit.MILLISECONDS)) {
                engine.addLog("SYSTEM", "Graceful terminate timed out; forcing process termination");
                process.destroyForcibly();
                process.waitFor(750, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        engine.exitCode = exitCode(process);
        engine.addLog("SYSTEM", "Terminate completed" + formatExitCode(engine.exitCode));
        return true;
    }

    private static void logBlock(String id, String direction, String block) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine == null || block == null) {
            return;
        }
        String[] lines = block.split("\\R", -1);
        for (String line : lines) {
            if (!line.isEmpty()) {
                engine.addLog(direction, line);
            }
        }
    }

    private static UciEngineProcessInfo toInfo(ManagedEngine engine) {
        Process process = engine.process;
        boolean alive = process != null && process.isAlive();
        Integer exitCode = alive ? null : (engine.exitCode != null ? engine.exitCode : exitCode(process));
        String state = alive ? "RUNNING" : (engine.closed ? "CLOSED" : "STOPPED");
        return new UciEngineProcessInfo(
                engine.id,
                engine.label,
                engine.engineType,
                engine.enginePath,
                process != null ? safePid(process) : null,
                alive,
                state,
                engine.createdAt.toString(),
                engine.processStartedAt != null ? engine.processStartedAt.toString() : null,
                engine.lastActivityAt.toString(),
                exitCode,
                engine.logCount.get());
    }

    private static Long safePid(Process process) {
        if (process == null) {
            return null;
        }
        try {
            return process.pid();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer exitCode(Process process) {
        if (process == null || process.isAlive()) {
            return null;
        }
        try {
            return process.exitValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String formatExitCode(Integer exitCode) {
        return exitCode == null ? "" : " (exit code " + exitCode + ")";
    }

    private static final class ManagedEngine {
        private final String id;
        private final String engineType;
        private final String enginePath;
        private final Instant createdAt = Instant.now();
        private final AtomicLong sequence = new AtomicLong();
        private final AtomicLong logCount = new AtomicLong();
        private final Deque<UciEngineLogEntry> log = new ArrayDeque<>();

        private volatile String label;
        private volatile Process process;
        private volatile Instant processStartedAt;
        private volatile Instant lastActivityAt = createdAt;
        private volatile Integer exitCode;
        private volatile boolean closed;

        private ManagedEngine(String id, String engineType, String enginePath) {
            this.id = id;
            this.engineType = engineType;
            this.enginePath = enginePath;
            this.label = engineType;
        }

        private synchronized void addLog(String direction, String message) {
            Instant now = Instant.now();
            lastActivityAt = now;
            long currentSequence = sequence.incrementAndGet();
            logCount.incrementAndGet();
            log.addLast(new UciEngineLogEntry(currentSequence, now.toString(), direction, message));
            while (log.size() > MAX_LOG_ENTRIES) {
                log.removeFirst();
            }
        }

        private synchronized List<UciEngineLogEntry> logSnapshot() {
            return new ArrayList<>(log);
        }

        private void touch() {
            lastActivityAt = Instant.now();
        }
    }
}
