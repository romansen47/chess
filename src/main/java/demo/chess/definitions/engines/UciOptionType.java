package demo.chess.definitions.engines;

import java.util.Locale;

public enum UciOptionType {
    CHECK,
    SPIN,
    COMBO,
    BUTTON,
    STRING;

    /**
     * Performs the from uci value operation.
     * @param value the value
     * @return the result of the operation
     */
    public static UciOptionType fromUciValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UCI option type must not be blank");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Performs the to uci value operation.
     * @return the result of the operation
     */
    public String toUciValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
