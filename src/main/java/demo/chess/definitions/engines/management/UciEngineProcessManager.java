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
 * In-process registry for UCI engine instances.
 *
 * <p>The concrete engine adapter remains the lifecycle owner. The registry observes
 * processes and protocol traffic, and stores a lifecycle callback supplied by that
 * owner. Normal stop requests therefore delegate to the engine adapter first so it
 * can send the UCI {@code quit} command and apply its normal timeout/escalation
 * policy. Raw process termination remains available only as an emergency fallback.</p>
 */
public final class UciEngineProcessManager {

    private static final int MAX_LOG_ENTRIES = 2000;
    private static final Map<String, ManagedEngine> ENGINES = new ConcurrentHashMap<>();

    /**
     * Creates a new UciEngineProcessManager instance.
     */
    private UciEngineProcessManager() {
    }

    /**
     * Performs the register operation.
     * @param engineType the engine type
     * @param enginePath the engine path
     * @return the result of the operation
     */
    public static String register(String engineType, String enginePath) {
        String id = UUID.randomUUID().toString();
        ManagedEngine engine = new ManagedEngine(id, engineType, enginePath);
        ENGINES.put(id, engine);
        engine.addLog("SYSTEM", "Engine instance registered");
        return id;
    }

    /**
     * Sets the label.
     * @param id the id
     * @param label the label
     */
    public static void setLabel(String id, String label) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine != null && label != null && !label.isBlank()) {
            engine.label = label.trim();
            engine.touch();
        }
    }

    /**
     * Registers the graceful lifecycle callback owned by the concrete engine adapter.
     * @param id managed engine id
     * @param gracefulCloser callback that closes the UCI engine through its owner
     */
    public static void setGracefulCloser(String id, Runnable gracefulCloser) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine != null) {
            engine.gracefulCloser = gracefulCloser;
            engine.touch();
        }
    }

    /**
     * Performs the attach process operation.
     * @param id the id
     * @param process the process
     */
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

    /**
     * Processes the ended.
     * @param id the id
     * @param process the process
     */
    public static void processEnded(String id, Process process) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine == null || engine.process != process) {
            return;
        }
        engine.exitCode = exitCode(process);
        engine.addLog("SYSTEM", "Process ended" + formatExitCode(engine.exitCode));
    }

    /**
     * Performs the mark closed operation.
     * @param id the id
     */
    public static void markClosed(String id) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine != null) {
            engine.closed = true;
            engine.gracefulCloser = null;
            engine.addLog("SYSTEM", "Engine instance closed");
        }
    }

    /**
     * Performs the log command operation.
     * @param id the id
     * @param commandBlock the command block
     */
    public static void logCommand(String id, String commandBlock) {
        logBlock(id, "COMMAND", commandBlock);
    }

    /**
     * Performs the log response operation.
     * @param id the id
     * @param response the response
     */
    public static void logResponse(String id, String response) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine != null && response != null) {
            engine.addLog("RESPONSE", response);
        }
    }

    /**
     * Performs the list operation.
     * @return the result of the operation
     */
    public static List<UciEngineProcessInfo> list() {
        return ENGINES.values().stream()
                .sorted(Comparator.comparing((ManagedEngine engine) -> engine.createdAt).reversed())
                .map(UciEngineProcessManager::toInfo)
                .toList();
    }

    /**
     * Performs the log operation.
     * @param id the id
     * @return the result of the operation
     */
    public static List<UciEngineLogEntry> log(String id) {
        ManagedEngine engine = ENGINES.get(id);
        return engine == null ? List.of() : engine.logSnapshot();
    }

    /**
     * Performs the terminate operation.
     * @param id the id
     * @return the result of the operation
     */
    public static boolean stop(String id) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine == null) {
            return false;
        }

        Process process = engine.process;
        if (process == null || !process.isAlive()) {
            engine.addLog("SYSTEM", "Stop requested, but no live process exists");
            return true;
        }

        Runnable gracefulCloser = engine.gracefulCloser;
        if (gracefulCloser != null) {
            engine.addLog("SYSTEM", "Graceful UCI stop requested for PID " + safePid(process));
            try {
                gracefulCloser.run();
            } catch (RuntimeException e) {
                engine.addLog("SYSTEM", "Graceful UCI stop failed: " + e.getMessage());
            }

            process = engine.process;
            if (process == null || !process.isAlive()) {
                engine.exitCode = exitCode(process);
                engine.addLog("SYSTEM", "Graceful UCI stop completed" + formatExitCode(engine.exitCode));
                return true;
            }
            engine.addLog("SYSTEM", "Graceful UCI stop did not end the process; escalating");
        }

        terminateProcess(engine, process, false);
        return true;
    }

    /**
     * Forcefully terminates the operating-system process without invoking the
     * engine owner's UCI shutdown path. Intended only for diagnostics/recovery.
     * @param id managed engine id
     * @return whether the engine id exists
     */
    public static boolean forceTerminate(String id) {
        ManagedEngine engine = ENGINES.get(id);
        if (engine == null) {
            return false;
        }

        Process process = engine.process;
        if (process == null || !process.isAlive()) {
            engine.addLog("SYSTEM", "Force termination requested, but no live process exists");
            return true;
        }

        terminateProcess(engine, process, true);
        return true;
    }

    /**
     * Backward-compatible alias. Termination now prefers the graceful UCI owner
     * callback and escalates only when necessary.
     * @param id managed engine id
     * @return whether the engine id exists
     */
    public static boolean terminate(String id) {
        return stop(id);
    }

    private static void terminateProcess(ManagedEngine engine, Process process, boolean forceImmediately) {
        engine.addLog(
                "SYSTEM",
                (forceImmediately ? "Force termination" : "Process termination")
                        + " requested for PID " + safePid(process));
        if (forceImmediately) {
            process.destroyForcibly();
        } else {
            process.destroy();
        }

        try {
            if (!process.waitFor(750, TimeUnit.MILLISECONDS)) {
                engine.addLog("SYSTEM", "Process termination timed out; forcing process termination");
                process.destroyForcibly();
                process.waitFor(750, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        engine.exitCode = exitCode(process);
        engine.addLog("SYSTEM", "Process termination completed" + formatExitCode(engine.exitCode));
    }

    /**
     * Performs the log block operation.
     * @param id the id
     * @param direction the direction
     * @param block the block
     */
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

    /**
     * Performs the to info operation.
     * @param engine the engine
     * @return the result of the operation
     */
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

    /**
     * Performs the safe pid operation.
     * @param process the process
     * @return the result of the operation
     */
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

    /**
     * Performs the exit code operation.
     * @param process the process
     * @return the result of the operation
     */
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

    /**
     * Formats the exit code.
     * @param exitCode the exit code
     * @return the result of the operation
     */
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
        private volatile Runnable gracefulCloser;

        /**
         * Creates a new ManagedEngine instance.
         * @param id the id
         * @param engineType the engine type
         * @param enginePath the engine path
         */
        private ManagedEngine(String id, String engineType, String enginePath) {
            this.id = id;
            this.engineType = engineType;
            this.enginePath = enginePath;
            this.label = engineType;
        }

        /**
         * Adds the log.
         * @param direction the direction
         * @param message the message
         */
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

        /**
         * Performs the log snapshot operation.
         * @return the result of the operation
         */
        private synchronized List<UciEngineLogEntry> logSnapshot() {
            return new ArrayList<>(log);
        }

        /**
         * Performs the touch operation.
         */
        private void touch() {
            lastActivityAt = Instant.now();
        }
    }
}
