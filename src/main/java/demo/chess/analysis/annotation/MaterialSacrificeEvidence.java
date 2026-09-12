package demo.chess.analysis.annotation;

final class MaterialSacrificeEvidence {

    private final MaterialSacrificeType type;
    private final double value;
    private final String acceptanceMoveUci;

    MaterialSacrificeEvidence(
            MaterialSacrificeType type,
            double value) {
        this(type, value, null);
    }

    MaterialSacrificeEvidence(
            MaterialSacrificeType type,
            double value,
            String acceptanceMoveUci) {
        this.type = type;
        this.value = value;
        this.acceptanceMoveUci = acceptanceMoveUci;
    }

    MaterialSacrificeType getType() {
        return type;
    }

    double getValue() {
        return value;
    }

    String getAcceptanceMoveUci() {
        return acceptanceMoveUci;
    }
}
