package demo.chess.definitions.engines;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable description of one UCI engine executable.
 *
 * The definition contains only properties that belong to the engine itself:
 * executable path, UCI identity and the option schema reported during the UCI
 * handshake. Context-specific values belong to an engine profile and are
 * combined with this definition only when a runtime {@link UciEngineConfig} is
 * required.
 */
public final class UciEngineDefinition {

    private final String engine;
    private final String engineName;
    private final String engineAuthor;
    private final LinkedHashMap<String, UciOption> options;

    public UciEngineDefinition(
            String engine,
            String engineName,
            String engineAuthor,
            Map<String, UciOption> options) {
        if (engine == null || engine.isBlank()) {
            throw new IllegalArgumentException("engine must not be blank");
        }
        this.engine = engine.trim();
        this.engineName = engineName == null || engineName.isBlank()
                ? this.engine
                : engineName.trim();
        this.engineAuthor = engineAuthor == null ? "" : engineAuthor.trim();
        this.options = copyOptions(options);
    }

    public UciEngineDefinition(UciEngineDefinition source) {
        this(
                Objects.requireNonNull(source, "source").engine,
                source.engineName,
                source.engineAuthor,
                source.options);
    }

    public String getEngine() {
        return engine;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getEngineAuthor() {
        return engineAuthor;
    }

    public Map<String, UciOption> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    public UciOption getOption(String name) {
        if (name == null) {
            return null;
        }

        UciOption exact = options.get(name);
        if (exact != null) {
            return exact;
        }

        for (Map.Entry<String, UciOption> entry : options.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public UciEngineDefinition copy() {
        return new UciEngineDefinition(this);
    }

    /**
     * Resolves this engine definition together with profile values to the
     * runtime configuration consumed by the existing UCI engine classes.
     */
    public UciEngineConfig createRuntimeConfig(
            EngineConfigType type,
            int depth,
            int moveTimeSeconds,
            Map<String, String> optionValues) {
        UciEngineConfig result = new UciEngineConfig(
                Objects.requireNonNull(type, "type"),
                engine,
                engineName,
                engineAuthor,
                options);
        result.setDepth(depth);
        result.setMoveTimeSeconds(moveTimeSeconds);

        if (optionValues != null) {
            for (Map.Entry<String, String> entry : optionValues.entrySet()) {
                UciOption option = result.getOption(entry.getKey());
                if (option == null) {
                    throw new IllegalArgumentException(
                            "Unknown UCI option '" + entry.getKey() + "' for engine " + engineName);
                }
                if (!option.isConfigurable()) {
                    throw new IllegalArgumentException(
                            "UCI button option '" + entry.getKey() + "' cannot be stored in a profile");
                }
                result.setOptionValue(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private static LinkedHashMap<String, UciOption> copyOptions(Map<String, UciOption> source) {
        LinkedHashMap<String, UciOption> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, UciOption> entry : source.entrySet()) {
            String name = Objects.requireNonNull(entry.getKey(), "UCI option name");
            UciOption option = Objects.requireNonNull(entry.getValue(), "UCI option " + name);
            result.put(name, option.copy());
        }
        return result;
    }
}
