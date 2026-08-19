package demo.chess.definitions.engines;

import java.util.Map;

/**
 * Runtime configuration for one concrete UCI engine invocation.
 *
 * The configuration deliberately has no player/evaluation/analysis type. The
 * caller decides how the configured engine is used. Search limits such as
 * depth and move time are runtime/use-case settings, while UCI option values
 * originate from the selected reusable engine profile.
 */
public interface EngineConfig {

    String getEngine();

    int getDepth();

    void setDepth(int depth);

    int getMoveTimeSeconds();

    void setMoveTimeSeconds(int moveTimeSeconds);

    Map<String, UciOption> getOptions();

    default UciOption getOption(String name) {
        if (name == null) {
            return null;
        }

        UciOption exact = getOptions().get(name);
        if (exact != null) {
            return exact;
        }

        for (Map.Entry<String, UciOption> entry : getOptions().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    default int getIntOption(String name, int fallback) {
        UciOption option = getOption(name);
        if (option == null || option.getValue() == null || option.getValue().isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(option.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    default String getStringOption(String name, String fallback) {
        UciOption option = getOption(name);
        return option == null || option.getValue() == null ? fallback : option.getValue();
    }

    String toUciSetOptionCommands();
}
