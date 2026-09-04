package demo.chess.definitions.engines;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fully resolved runtime configuration for a UCI engine process.
 *
 * There is intentionally no Player/Evaluation/DeepAnalysis discriminator here.
 * The same configured profile can be consumed by any of those use cases.
 */
public class UciEngineConfig implements EngineConfig {

    private final String engine;
    private final String engineName;
    private final String engineAuthor;
    private final LinkedHashMap<String, UciOption> options;

    private int depth;
    private int moveTimeSeconds;

    /**
     * Creates a new UciEngineConfig instance.
     * @param engine the engine
     * @param engineName the engine name
     * @param engineAuthor the engine author
     * @param options the options
     */
    public UciEngineConfig(
            String engine,
            String engineName,
            String engineAuthor,
            Map<String, UciOption> options) {
        if (engine == null || engine.isBlank()) {
            throw new IllegalArgumentException("engine must not be blank");
        }
        this.engine = engine.trim();
        this.engineName = engineName == null || engineName.isBlank() ? this.engine : engineName.trim();
        this.engineAuthor = engineAuthor == null ? "" : engineAuthor.trim();
        this.options = copyOptions(options);
    }

    /**
     * Creates a new UciEngineConfig instance.
     * @param source the source
     */
    public UciEngineConfig(UciEngineConfig source) {
        this(
                source.engine,
                source.engineName,
                source.engineAuthor,
                source.options);
        this.depth = source.depth;
        this.moveTimeSeconds = source.moveTimeSeconds;
    }

    /**
     * Returns the engine.
     * @return the engine
     */
    @Override
    public String getEngine() {
        return engine;
    }

    /**
     * Returns the engine name.
     * @return the engine name
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Returns the engine author.
     * @return the engine author
     */
    public String getEngineAuthor() {
        return engineAuthor;
    }

    /**
     * Returns the depth.
     * @return the depth
     */
    @Override
    public int getDepth() {
        return depth;
    }

    /**
     * Sets the depth.
     * @param depth the depth
     */
    @Override
    public void setDepth(int depth) {
        this.depth = Math.max(0, depth);
    }

    /**
     * Returns the move time seconds.
     * @return the move time seconds
     */
    @Override
    public int getMoveTimeSeconds() {
        return moveTimeSeconds;
    }

    /**
     * Sets the move time seconds.
     * @param moveTimeSeconds the move time seconds
     */
    @Override
    public void setMoveTimeSeconds(int moveTimeSeconds) {
        this.moveTimeSeconds = Math.max(0, moveTimeSeconds);
    }

    /**
     * Returns the options.
     * @return the options
     */
    @Override
    public Map<String, UciOption> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    /**
     * Sets the option value.
     * @param name the name
     * @param value the value
     */
    public void setOptionValue(String name, String value) {
        UciOption option = getOption(name);
        if (option == null) {
            throw new IllegalArgumentException("Unknown UCI option: " + name);
        }
        option.setValue(value);
    }

    /**
     * Performs the copy operation.
     * @return the result of the operation
     */
    public UciEngineConfig copy() {
        return new UciEngineConfig(this);
    }

    /**
     * Performs the to uci set option commands operation.
     * @return the result of the operation
     */
    @Override
    public String toUciSetOptionCommands() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, UciOption> entry : options.entrySet()) {
            UciOption option = entry.getValue();
            if (option == null || !option.isConfigurable()) {
                continue;
            }
            String command = option.toSetOptionCommand(entry.getKey());
            if (command.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(command);
        }
        return result.toString();
    }

    /**
     * Performs the copy options operation.
     * @param source the source
     * @return the result of the operation
     */
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
