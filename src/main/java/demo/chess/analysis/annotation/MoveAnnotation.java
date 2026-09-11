package demo.chess.analysis.annotation;

/**
 * Domain result of classifying one played move from finite DeepAnalysis data.
 */
public final class MoveAnnotation {

    private final MoveAnnotationKind kind;
    private final double bestEvaluation;
    private final Double loss;
    private final Double secondBestEvaluation;
    private final BrilliantReason brilliantReason;
    private final Double materialInvestment;
    private final Integer earlyDepth;
    private final Integer earlyRank;
    private final Integer finalDepth;
    private final Integer finalRank;

    public MoveAnnotation(
            MoveAnnotationKind kind,
            double bestEvaluation,
            Double loss,
            Double secondBestEvaluation,
            BrilliantReason brilliantReason,
            Double materialInvestment,
            Integer earlyDepth,
            Integer earlyRank,
            Integer finalDepth,
            Integer finalRank) {
        this.kind = kind;
        this.bestEvaluation = bestEvaluation;
        this.loss = loss;
        this.secondBestEvaluation = secondBestEvaluation;
        this.brilliantReason = brilliantReason;
        this.materialInvestment = materialInvestment;
        this.earlyDepth = earlyDepth;
        this.earlyRank = earlyRank;
        this.finalDepth = finalDepth;
        this.finalRank = finalRank;
    }

    public MoveAnnotationKind getKind() {
        return kind;
    }

    public double getBestEvaluation() {
        return bestEvaluation;
    }

    public Double getLoss() {
        return loss;
    }

    public Double getSecondBestEvaluation() {
        return secondBestEvaluation;
    }

    public BrilliantReason getBrilliantReason() {
        return brilliantReason;
    }

    public Double getMaterialInvestment() {
        return materialInvestment;
    }

    public Integer getEarlyDepth() {
        return earlyDepth;
    }

    public Integer getEarlyRank() {
        return earlyRank;
    }

    public Integer getFinalDepth() {
        return finalDepth;
    }

    public Integer getFinalRank() {
        return finalRank;
    }
}
