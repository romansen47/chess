package demo.chess.definitions.engines;

public enum EngineConfigType {
    PLAYER,
    EVALUATION,
    DEEP_ANALYSIS;

    public static EngineConfigType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Engine config type must not be blank");
        }
        for (EngineConfigType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported engine config type: " + value);
    }
}
