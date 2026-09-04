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

    /**
     * Returns the engine.
     * @return the engine
     */
    String getEngine();

    /**
     * Returns the depth.
     * @return the depth
     */
    int getDepth();

    /**
     * Sets the depth.
     * @param depth the depth
     */
    void setDepth(int depth);

    /**
     * Returns the move time seconds.
     * @return the move time seconds
     */
    int getMoveTimeSeconds();

    /**
     * Sets the move time seconds.
     * @param moveTimeSeconds the move time seconds
     */
    void setMoveTimeSeconds(int moveTimeSeconds);

    /**
     * Returns the options.
     * @return the options
     */
    Map<String, UciOption> getOptions();

    /**
     * Returns the option.
     * @param name the name
     * @return the option
     */
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

    /**
     * Returns the int option.
     * @param name the name
     * @param fallback the fallback
     * @return the int option
     */
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

    /**
     * Returns the string option.
     * @param name the name
     * @param fallback the fallback
     * @return the string option
     */
    default String getStringOption(String name, String fallback) {
        UciOption option = getOption(name);
        return option == null || option.getValue() == null ? fallback : option.getValue();
    }

    /**
     * Performs the to uci set option commands operation.
     * @return the result of the operation
     */
    String toUciSetOptionCommands();
}
