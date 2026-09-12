package demo.chess.analysis.annotation;

final class MaterialSacrificeEvidence {

    private final MaterialSacrificeType type;
    private final double value;

    MaterialSacrificeEvidence(
            MaterialSacrificeType type,
            double value) {
        this.type = type;
        this.value = value;
    }

    MaterialSacrificeType getType() {
        return type;
    }

    double getValue() {
        return value;
    }
}
