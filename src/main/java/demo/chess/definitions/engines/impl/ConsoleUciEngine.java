package demo.chess.definitions.engines.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.engines.ChessEngine;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.UciSystemOptions;
import demo.chess.definitions.engines.management.UciEngineProcessManager;
import demo.chess.game.Game;

/**
 * Base adapter for native UCI engine processes.
 *
 * <p>The adapter owns protocol-level state that must not be stored in reusable
 * engine profiles. Chess960 mode is derived from the current game and is only
 * sent when the engine advertised the standard {@code UCI_Chess960} option
 * during its UCI handshake.</p>
 */
public abstract class ConsoleUciEngine implements ChessEngine {

    protected static final Logger logger = LogManager.getLogger(ConsoleUciEngine.class);
    private static final long UCI_HANDSHAKE_TIMEOUT_SECONDS = 5L;

    protected Process uciEngineProcess;
    protected PrintWriter writer;
    protected BufferedReader reader;

    private final String enginePath;
    private final String managementId;
    private Boolean lastChess960Mode;
    private boolean chess960CapabilityAdvertised;

    public ConsoleUciEngine(String path) throws Exception {
        this.enginePath = path;
        this.managementId = UciEngineProcessManager.register(getClass().getSimpleName(), path);
        startProcess();
    }

    public final String getManagementId() {
        return managementId;
    }

    public final void setManagementLabel(String label) {
        UciEngineProcessManager.setLabel(managementId, label);
    }

    protected final String getEnginePath() {
        return enginePath;
    }

    protected synchronized void restartProcess() throws Exception {
        destroyCurrentProcess();
        startProcess();
    }

    protected synchronized void applyConfig(EngineConfig config) throws IOException, InterruptedException {
        if (config == null) return;
        if (!enginePath.equals(config.getEngine())) {
            throw new IllegalArgumentException(
                    "Engine config belongs to '" + config.getEngine()
                            + "' but running engine is '" + enginePath + "'");
        }

        String commands = config.toUciSetOptionCommands();
        if (!commands.isBlank()) writer.println(commands);
        writer.println("isready");
        writer.flush();
        try {
            awaitLine("readyok", UCI_HANDSHAKE_TIMEOUT_SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            throw new IOException("Engine did not become ready: " + enginePath, e);
        }
    }

    /**
     * Configures Chess960 protocol mode from the domain game.
     *
     * <p>Engines that do not advertise {@code UCI_Chess960} remain perfectly
     * valid for classical chess and are not sent an unknown option. A
     * non-standard starting position, however, is rejected before a search is
     * started because interpreting its FEN and castling moves as classical
     * chess would be unsafe.</p>
     */
    protected synchronized void prepareForGame(Game game) throws IOException, InterruptedException {
        boolean chess960 = game != null
                && game.getStartingPosition() != null
                && !game.getStartingPosition().isStandard();

        if (chess960 && !chess960CapabilityAdvertised) {
            throw new IOException(
                    "Engine does not advertise " + UciSystemOptions.CHESS960
                            + " and cannot be used for Chess960: " + enginePath);
        }
        if (!chess960CapabilityAdvertised) {
            lastChess960Mode = Boolean.FALSE;
            return;
        }
        if (lastChess960Mode != null && lastChess960Mode == chess960) return;

        writer.println("setoption name " + UciSystemOptions.CHESS960 + " value " + chess960);
        writer.println("isready");
        writer.flush();
        try {
            awaitLine("readyok", UCI_HANDSHAKE_TIMEOUT_SECONDS);
            lastChess960Mode = chess960;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            throw new IOException("Engine did not accept Chess960 mode: " + enginePath, e);
        }
    }

    private void startProcess() throws Exception {
        lastChess960Mode = null;
        chess960CapabilityAdvertised = false;
        uciEngineProcess = new ProcessBuilder(enginePath).redirectErrorStream(true).start();
        UciEngineProcessManager.attachProcess(managementId, uciEngineProcess);
        writer = new LoggingPrintWriter(new OutputStreamWriter(uciEngineProcess.getOutputStream()), managementId);
        reader = new LoggingBufferedReader(new InputStreamReader(uciEngineProcess.getInputStream()), managementId);
        try {
            writer.println("uci");
            writer.flush();
            awaitUciHandshake(UCI_HANDSHAKE_TIMEOUT_SECONDS);
        } catch (Exception e) {
            destroyCurrentProcess();
            throw new IllegalStateException("UCI handshake failed for engine " + enginePath, e);
        }
    }

    /**
     * Reads the complete UCI handshake so protocol capabilities are retained
     * instead of being discarded while waiting for {@code uciok}.
     */
    private void awaitUciHandshake(long timeoutSeconds) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "uci-await-uciok");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<Boolean> future = executor.submit(() -> {
                boolean supportsChess960 = false;
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (isChess960OptionLine(trimmed)) {
                        supportsChess960 = true;
                    }
                    if ("uciok".equals(trimmed)) {
                        return supportsChess960;
                    }
                }
                throw new IllegalStateException("Engine output ended before uciok");
            });
            chess960CapabilityAdvertised = future.get(timeoutSeconds, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean isChess960OptionLine(String line) {
        if (line == null) return false;
        String prefix = "option name " + UciSystemOptions.CHESS960 + " ";
        return line.length() >= prefix.length()
                && line.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private void awaitLine(String expected, long timeoutSeconds) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "uci-await-" + expected);
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<Void> future = executor.submit(() -> {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (expected.equals(line.trim())) return null;
                }
                throw new IllegalStateException("Engine output ended before " + expected);
            });
            future.get(timeoutSeconds, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (writer != null) {
                writer.println("quit");
                writer.flush();
            }
        } catch (Exception e) {
            logger.debug("Could not send quit to UCI engine", e);
        }
        try {
            if (uciEngineProcess != null && uciEngineProcess.isAlive()
                    && !uciEngineProcess.waitFor(1, TimeUnit.SECONDS)) {
                uciEngineProcess.destroy();
                if (!uciEngineProcess.waitFor(1, TimeUnit.SECONDS)) {
                    uciEngineProcess.destroyForcibly();
                    uciEngineProcess.waitFor(1, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (uciEngineProcess != null) uciEngineProcess.destroyForcibly();
        } finally {
            closeStreams();
            UciEngineProcessManager.processEnded(managementId, uciEngineProcess);
            UciEngineProcessManager.markClosed(managementId);
        }
    }

    private void destroyCurrentProcess() {
        Process process = uciEngineProcess;
        lastChess960Mode = null;
        chess960CapabilityAdvertised = false;
        closeStreams();
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(500, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        UciEngineProcessManager.processEnded(managementId, process);
    }

    private void closeStreams() {
        try {
            if (writer != null) writer.close();
        } catch (Exception e) {
            logger.debug("Could not close UCI engine writer", e);
        }
        try {
            if (reader != null) reader.close();
        } catch (Exception e) {
            logger.debug("Could not close UCI engine reader", e);
        }
    }

    protected abstract StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config);

    protected PrintWriter getWriter() {
        return writer;
    }

    private static final class LoggingPrintWriter extends PrintWriter {
        private final String managementId;

        private LoggingPrintWriter(Writer out, String managementId) {
            super(out, true);
            this.managementId = managementId;
        }

        @Override
        public void println(String value) {
            UciEngineProcessManager.logCommand(managementId, value);
            super.println(value);
        }
    }

    private static final class LoggingBufferedReader extends BufferedReader {
        private final String managementId;

        private LoggingBufferedReader(Reader in, String managementId) {
            super(in);
            this.managementId = managementId;
        }

        @Override
        public String readLine() throws java.io.IOException {
            String line = super.readLine();
            if (line != null) UciEngineProcessManager.logResponse(managementId, line);
            return line;
        }
    }
}
