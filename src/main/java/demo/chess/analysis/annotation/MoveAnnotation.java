package demo.chess.analysis.annotation;

/**
 * Domain result of classifying one played move from finite DeepAnalysis data.
 */
public final class MoveAnnotation {

    private final MoveAnnotationKind kind;
    private final double bestEvaluation;
    private final Double winChanceLoss;
    private final Double secondBestEvaluation;
    private final BrilliantReason brilliantReason;
    private final Double materialInvestment;
    private final Integer earlyDepth;
    private final Integer earlyRank;
    private final Integer finalDepth;
    private final Integer finalRank;
    private final Boolean givesCheck;
    private final Double earlyRegret;
    private final Double middleRegret;
    private final Double lateRegret;
    private final Double earlyStrength;
    private final Double middleStrength;
    private final Double lateStrength;

    public MoveAnnotation(
            MoveAnnotationKind kind,
            double bestEvaluation,
            Double winChanceLoss,
            Double secondBestEvaluation,
            BrilliantReason brilliantReason,
            Double materialInvestment,
            Integer earlyDepth,
            Integer earlyRank,
            Integer finalDepth,
            Integer finalRank) {
        this(
                kind,
                bestEvaluation,
                winChanceLoss,
                secondBestEvaluation,
                brilliantReason,
                materialInvestment,
                earlyDepth,
                earlyRank,
                finalDepth,
                finalRank,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public MoveAnnotation(
            MoveAnnotationKind kind,
            double bestEvaluation,
            Double winChanceLoss,
            Double secondBestEvaluation,
            BrilliantReason brilliantReason,
            Double materialInvestment,
            Integer earlyDepth,
            Integer earlyRank,
            Integer finalDepth,
            Integer finalRank,
            Boolean givesCheck,
            Double earlyRegret,
            Double middleRegret,
            Double lateRegret,
            Double earlyStrength,
            Double middleStrength,
            Double lateStrength) {
        this.kind = kind;
        this.bestEvaluation = bestEvaluation;
        this.winChanceLoss = winChanceLoss;
        this.secondBestEvaluation = secondBestEvaluation;
        this.brilliantReason = brilliantReason;
        this.materialInvestment = materialInvestment;
        this.earlyDepth = earlyDepth;
        this.earlyRank = earlyRank;
        this.finalDepth = finalDepth;
        this.finalRank = finalRank;
        this.givesCheck = givesCheck;
        this.earlyRegret = earlyRegret;
        this.middleRegret = middleRegret;
        this.lateRegret = lateRegret;
        this.earlyStrength = earlyStrength;
        this.middleStrength = middleStrength;
        this.lateStrength = lateStrength;
    }

    public MoveAnnotationKind getKind() {
        return kind;
    }

    public double getBestEvaluation() {
        return bestEvaluation;
    }

    public Double getWinChanceLoss() {
        return winChanceLoss;
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

    public Boolean getGivesCheck() {
        return givesCheck;
    }

    public Double getEarlyRegret() {
        return earlyRegret;
    }

    public Double getMiddleRegret() {
        return middleRegret;
    }

    public Double getLateRegret() {
        return lateRegret;
    }

    public Double getEarlyStrength() {
        return earlyStrength;
    }

    public Double getMiddleStrength() {
        return middleStrength;
    }

    public Double getLateStrength() {
        return lateStrength;
    }
}
