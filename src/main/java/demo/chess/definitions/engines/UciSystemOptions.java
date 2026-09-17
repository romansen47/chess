package demo.chess.definitions.engines;

import java.util.Map;

/**
 * Names and classification rules for UCI options that are controlled by the
 * application runtime rather than by reusable user profiles.
 *
 * <p>System-managed options describe protocol or game context. Persisting them
 * as ordinary profile values would allow a profile to contradict the current
 * game. In particular, {@value #CHESS960} is derived from the game's
 * {@code ChessStartingPosition} and must never be restored from a profile.</p>
 */
public final class UciSystemOptions {

    /** Standard UCI option used by engines to advertise and enable Chess960. */
    public static final String CHESS960 = "UCI_Chess960";

    private UciSystemOptions() {
    }

    /**
     * Returns whether an option is controlled by runtime context rather than a
     * reusable engine profile.
     *
     * @param name UCI option name
     * @return {@code true} for a system-managed option
     */
    public static boolean isSystemManaged(String name) {
        return name != null && CHESS960.equalsIgnoreCase(name.trim());
    }

    /**
     * Returns whether an advertised option map contains the standard Chess960
     * capability switch with the expected UCI {@code check} type.
     *
     * @param options engine option schema
     * @return whether Chess960 is advertised by the engine
     */
    public static boolean supportsChess960(Map<String, UciOption> options) {
        if (options == null) return false;
        for (Map.Entry<String, UciOption> entry : options.entrySet()) {
            if (!CHESS960.equalsIgnoreCase(entry.getKey())) continue;
            UciOption option = entry.getValue();
            return option != null && option.getType() == UciOptionType.CHECK;
        }
        return false;
    }
}
