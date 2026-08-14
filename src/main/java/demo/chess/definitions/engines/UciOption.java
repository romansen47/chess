package demo.chess.definitions.engines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UciOption {

    private final UciOptionType type;
    private final String defaultValue;
    private final Integer min;
    private final Integer max;
    private final List<String> vars;
    private String value;

    public UciOption(
            UciOptionType type,
            String defaultValue,
            String value,
            Integer min,
            Integer max,
            List<String> vars) {
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValue = normalizeDefault(type, defaultValue);
        this.min = min;
        this.max = max;
        this.vars = vars == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(vars));
        this.value = normalizeValue(type, value != null ? value : this.defaultValue);
        validateValue(this.value);
    }

    public UciOption(UciOption source) {
        this(
                source.type,
                source.defaultValue,
                source.value,
                source.min,
                source.max,
                source.vars);
    }

    public UciOptionType getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        String normalized = normalizeValue(type, value);
        validateValue(normalized);
        this.value = normalized;
    }

    public Integer getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }

    public List<String> getVars() {
        return vars;
    }

    public boolean isConfigurable() {
        return type != UciOptionType.BUTTON;
    }

    public UciOption copy() {
        return new UciOption(this);
    }

    public String toSetOptionCommand(String name) {
        if (!isConfigurable()) {
            return "";
        }

        StringBuilder command = new StringBuilder("setoption name ").append(name);
        if (value != null) {
            command.append(" value");
            if (!value.isEmpty()) {
                command.append(' ').append(value);
            }
        }
        return command.toString();
    }

    private void validateValue(String candidate) {
        if (type == UciOptionType.BUTTON) {
            return;
        }

        if (candidate == null) {
            throw new IllegalArgumentException("UCI option value must not be null for type " + type);
        }

        switch (type) {
            case CHECK -> {
                if (!"true".equalsIgnoreCase(candidate) && !"false".equalsIgnoreCase(candidate)) {
                    throw new IllegalArgumentException("Expected boolean UCI option value but got: " + candidate);
                }
            }
            case SPIN -> {
                int parsed;
                try {
                    parsed = Integer.parseInt(candidate.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Expected integer UCI option value but got: " + candidate, e);
                }
                if (min != null && parsed < min) {
                    throw new IllegalArgumentException("UCI option value " + parsed + " is below minimum " + min);
                }
                if (max != null && parsed > max) {
                    throw new IllegalArgumentException("UCI option value " + parsed + " is above maximum " + max);
                }
            }
            case COMBO -> {
                if (!vars.isEmpty() && !vars.contains(candidate)) {
                    throw new IllegalArgumentException(
                            "UCI combo option value '" + candidate + "' is not one of " + vars);
                }
            }
            case STRING -> {
                // Every string, including an empty string, is valid.
            }
            case BUTTON -> {
                // handled above
            }
        }
    }

    private static String normalizeDefault(UciOptionType type, String value) {
        if (type == UciOptionType.BUTTON) {
            return null;
        }
        if (value == null) {
            return type == UciOptionType.STRING ? "" : null;
        }
        if (type == UciOptionType.STRING && "<empty>".equalsIgnoreCase(value.trim())) {
            return "";
        }
        return normalizeValue(type, value);
    }

    private static String normalizeValue(UciOptionType type, String value) {
        if (type == UciOptionType.BUTTON) {
            return null;
        }
        if (value == null) {
            return type == UciOptionType.STRING ? "" : null;
        }
        String normalized = type == UciOptionType.STRING ? value : value.trim();
        if (type == UciOptionType.CHECK) {
            return normalized.toLowerCase();
        }
        return normalized;
    }
}
