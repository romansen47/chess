package demo.chess.definitions.engines;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates a {@link UciEngineDefinition} from the UCI handshake of one concrete
 * executable. Profile-specific settings are deliberately not part of inspection.
 */
public final class UciEngineInspector {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(4);
    private static final Pattern OPTION_TYPE_PATTERN = Pattern.compile(
            "\\s+type\\s+(check|spin|combo|button|string)(?:\\s+|$)",
            Pattern.CASE_INSENSITIVE);

    private UciEngineInspector() {
    }

    public static UciEngineDefinition inspect(String enginePath) throws Exception {
        return inspect(enginePath, DEFAULT_TIMEOUT);
    }

    public static UciEngineDefinition inspect(String enginePath, Duration timeout) throws Exception {
        if (enginePath == null || enginePath.isBlank()) {
            throw new IllegalArgumentException("enginePath must not be blank");
        }

        String normalizedPath = enginePath.trim();
        Process process = null;
        ExecutorService readerExecutor = null;

        try {
            process = new ProcessBuilder(normalizedPath)
                    .redirectErrorStream(true)
                    .start();

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8));

            readerExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "uci-engine-inspector");
                thread.setDaemon(true);
                return thread;
            });

            Future<UciHandshake> handshakeFuture = readerExecutor.submit(() -> readHandshake(reader));

            writer.println("uci");
            writer.flush();

            UciHandshake handshake = handshakeFuture.get(
                    Math.max(1L, timeout.toMillis()),
                    TimeUnit.MILLISECONDS);

            writer.println("quit");
            writer.flush();

            return new UciEngineDefinition(
                    normalizedPath,
                    fallbackName(handshake.engineName, normalizedPath),
                    handshake.engineAuthor,
                    handshake.options);
        } finally {
            if (readerExecutor != null) {
                readerExecutor.shutdownNow();
            }
            if (process != null && process.isAlive()) {
                process.destroy();
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(500, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * Compatibility helper for callers that still need a fully resolved runtime
     * configuration directly after inspection. New code should persist the
     * returned engine definition and create profiles separately.
     */
    public static UciEngineConfig inspect(String enginePath, EngineConfigType type) throws Exception {
        return inspect(enginePath).createRuntimeConfig(0, 0, Map.of());
    }

    public static UciEngineConfig inspect(
            String enginePath,
            EngineConfigType type,
            Duration timeout) throws Exception {
        return inspect(enginePath, timeout).createRuntimeConfig(0, 0, Map.of());
    }

    static Map.Entry<String, UciOption> parseOptionLine(String line) {
        if (line == null || !line.startsWith("option name ")) {
            throw new IllegalArgumentException("Not a UCI option line: " + line);
        }

        String body = line.substring("option name ".length());
        Matcher typeMatcher = OPTION_TYPE_PATTERN.matcher(body);
        if (!typeMatcher.find()) {
            throw new IllegalArgumentException("UCI option line has no supported type: " + line);
        }

        String name = body.substring(0, typeMatcher.start()).trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("UCI option line has no name: " + line);
        }

        UciOptionType type = UciOptionType.fromUciValue(typeMatcher.group(1));
        String metadata = body.substring(typeMatcher.end()).trim();

        ParsedMetadata parsed = parseMetadata(type, metadata);
        String defaultValue = parsed.defaultValue;
        if (type == UciOptionType.STRING && defaultValue == null) {
            defaultValue = "";
        }
        if (type == UciOptionType.STRING && defaultValue != null
                && "<empty>".equalsIgnoreCase(defaultValue.trim())) {
            defaultValue = "";
        }

        UciOption option = new UciOption(
                type,
                defaultValue,
                defaultValue,
                parsed.min,
                parsed.max,
                parsed.vars);
        return Map.entry(name, option);
    }

    private static UciHandshake readHandshake(BufferedReader reader) throws Exception {
        String engineName = null;
        String engineAuthor = "";
        LinkedHashMap<String, UciOption> options = new LinkedHashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("id name ")) {
                engineName = line.substring("id name ".length()).trim();
                continue;
            }
            if (line.startsWith("id author ")) {
                engineAuthor = line.substring("id author ".length()).trim();
                continue;
            }
            if (line.startsWith("option name ")) {
                Map.Entry<String, UciOption> parsed = parseOptionLine(line);
                options.put(parsed.getKey(), parsed.getValue());
                continue;
            }
            if ("uciok".equals(line.trim())) {
                return new UciHandshake(engineName, engineAuthor, options);
            }
        }

        throw new IllegalStateException("Engine closed its output before uciok");
    }

    private static ParsedMetadata parseMetadata(UciOptionType type, String metadata) {
        ParsedMetadata result = new ParsedMetadata();
        String text = metadata == null ? "" : metadata.trim();
        if (text.isEmpty() || type == UciOptionType.BUTTON) {
            return result;
        }

        if (type == UciOptionType.STRING) {
            if (text.equals("default")) {
                result.defaultValue = "";
            } else if (text.startsWith("default ")) {
                result.defaultValue = text.substring("default ".length());
            }
            return result;
        }

        if (type == UciOptionType.CHECK) {
            if (text.startsWith("default ")) {
                result.defaultValue = text.substring("default ".length()).trim();
            }
            return result;
        }

        if (type == UciOptionType.SPIN) {
            int minIndex = text.indexOf(" min ");
            int maxIndex = text.indexOf(" max ");
            if (text.startsWith("default ")) {
                int defaultEnd = minIndex >= 0 ? minIndex : text.length();
                result.defaultValue = text.substring("default ".length(), defaultEnd).trim();
            }
            if (minIndex >= 0) {
                int minValueStart = minIndex + " min ".length();
                int minValueEnd = maxIndex > minIndex ? maxIndex : text.length();
                result.min = parseInteger(text.substring(minValueStart, minValueEnd).trim(), "min");
            }
            if (maxIndex >= 0) {
                result.max = parseInteger(text.substring(maxIndex + " max ".length()).trim(), "max");
            }
            return result;
        }

        if (type == UciOptionType.COMBO) {
            int firstVar = text.indexOf(" var ");
            if (text.startsWith("default ")) {
                int defaultEnd = firstVar >= 0 ? firstVar : text.length();
                result.defaultValue = text.substring("default ".length(), defaultEnd).trim();
            }
            int cursor = firstVar;
            while (cursor >= 0) {
                int valueStart = cursor + " var ".length();
                int nextVar = text.indexOf(" var ", valueStart);
                String value = text.substring(valueStart, nextVar >= 0 ? nextVar : text.length()).trim();
                result.vars.add(value);
                cursor = nextVar;
            }
        }
        return result;
    }

    private static Integer parseInteger(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid UCI option " + label + " value: " + value, e);
        }
    }

    private static String fallbackName(String engineName, String path) {
        if (engineName != null && !engineName.isBlank()) {
            return engineName.trim();
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 && slash + 1 < path.length() ? path.substring(slash + 1) : path;
    }

    private static final class UciHandshake {
        private final String engineName;
        private final String engineAuthor;
        private final Map<String, UciOption> options;

        private UciHandshake(String engineName, String engineAuthor, Map<String, UciOption> options) {
            this.engineName = engineName;
            this.engineAuthor = engineAuthor == null ? "" : engineAuthor;
            this.options = options;
        }
    }

    private static final class ParsedMetadata {
        private String defaultValue;
        private Integer min;
        private Integer max;
        private final List<String> vars = new ArrayList<>();
    }

}
