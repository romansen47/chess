package demo.chess.definitions.engines;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class UciEngineConfig implements EngineConfig {

    private final EngineConfigType type;
    private final String engine;
    private final String engineName;
    private final String engineAuthor;
    private final LinkedHashMap<String, UciOption> options;

    private int depth;
    private int moveTimeSeconds;

    public UciEngineConfig(
            EngineConfigType type,
            String engine,
            String engineName,
            String engineAuthor,
            Map<String, UciOption> options) {
        this.type = Objects.requireNonNull(type, "type");
        if (engine == null || engine.isBlank()) {
            throw new IllegalArgumentException("engine must not be blank");
        }
        this.engine = engine.trim();
        this.engineName = engineName == null || engineName.isBlank() ? this.engine : engineName.trim();
        this.engineAuthor = engineAuthor == null ? "" : engineAuthor.trim();
        this.options = copyOptions(options);
    }

    public UciEngineConfig(UciEngineConfig source) {
        this(
                source.type,
                source.engine,
                source.engineName,
                source.engineAuthor,
                source.options);
        this.depth = source.depth;
        this.moveTimeSeconds = source.moveTimeSeconds;
    }

    @Override
    public EngineConfigType getType() {
        return type;
    }

    @Override
    public String getEngine() {
        return engine;
    }

    public String getEngineName() {
        return engineName;
    }

    public String getEngineAuthor() {
        return engineAuthor;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = Math.max(0, depth);
    }

    @Override
    public int getMoveTimeSeconds() {
        return moveTimeSeconds;
    }

    @Override
    public void setMoveTimeSeconds(int moveTimeSeconds) {
        this.moveTimeSeconds = Math.max(0, moveTimeSeconds);
    }

    @Override
    public Map<String, UciOption> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    public void setOptionValue(String name, String value) {
        UciOption option = getOption(name);
        if (option == null) {
            throw new IllegalArgumentException("Unknown UCI option: " + name);
        }
        option.setValue(value);
    }

    public UciEngineConfig copy() {
        return new UciEngineConfig(this);
    }

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
