package demo.chess.analysis.annotation;

final class MaterialSacrificeEvidence {

    private final MaterialSacrificeType type;
    private final double value;
    private final String acceptanceMoveUci;
    private final MaterialTrajectoryAnalyzer.Trajectory trajectory;

    MaterialSacrificeEvidence(
            MaterialSacrificeType type,
            double value) {
        this(type, value, null);
    }

    MaterialSacrificeEvidence(
            MaterialSacrificeType type,
            double value,
            String acceptanceMoveUci) {
        this(type, value, acceptanceMoveUci, null);
    }

    MaterialSacrificeEvidence(
            MaterialSacrificeType type,
            double value,
            String acceptanceMoveUci,
            MaterialTrajectoryAnalyzer.Trajectory trajectory) {
        this.type = type;
        this.value = value;
        this.acceptanceMoveUci = acceptanceMoveUci;
        this.trajectory = trajectory;
    }

    MaterialTrajectoryAnalyzer.Trajectory getTrajectory() {
        return trajectory;
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
