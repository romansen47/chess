package demo.chess.definitions.engines;

import java.util.Locale;

public enum UciOptionType {
    CHECK,
    SPIN,
    COMBO,
    BUTTON,
    STRING;

    public static UciOptionType fromUciValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UCI option type must not be blank");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public String toUciValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
