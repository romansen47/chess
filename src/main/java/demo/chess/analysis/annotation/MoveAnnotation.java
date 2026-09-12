package demo.chess.analysis.annotation;

/**
 * Domain result of classifying one played move from finite DeepAnalysis data.
 */
public final class MoveAnnotation {

    private final MoveAnnotationKind kind;
    private final double bestEvaluation;
    private final Double winChanceLoss;
    private final Double secondBestEvaluation;

    private final ExtraordinaryReason extraordinaryReason;
    private final Double materialInvestment;
    private final MaterialSacrificeType sacrificeType;

    private final Integer earlyDepth;
    private final Integer earlyRank;
    private final Integer finalDepth;
    private final Integer finalRank;
    private final Boolean givesCheck;
    private final Double earlyRegret;
    private final Double earlyStrength;
    private final Double finalStrength;

    public MoveAnnotation(
            MoveAnnotationKind kind,
            double bestEvaluation,
            Double winChanceLoss,
            Double secondBestEvaluation) {
        this(
                kind,
                bestEvaluation,
                winChanceLoss,
                secondBestEvaluation,
                null,
                null,
                null,
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
            ExtraordinaryReason extraordinaryReason,
            Double materialInvestment,
            MaterialSacrificeType sacrificeType,
            Integer earlyDepth,
            Integer earlyRank,
            Integer finalDepth,
            Integer finalRank,
            Boolean givesCheck,
            Double earlyRegret,
            Double earlyStrength,
            Double finalStrength) {
        this.kind = kind;
        this.bestEvaluation = bestEvaluation;
        this.winChanceLoss = winChanceLoss;
        this.secondBestEvaluation = secondBestEvaluation;
        this.extraordinaryReason = extraordinaryReason;
        this.materialInvestment = materialInvestment;
        this.sacrificeType = sacrificeType;
        this.earlyDepth = earlyDepth;
        this.earlyRank = earlyRank;
        this.finalDepth = finalDepth;
        this.finalRank = finalRank;
        this.givesCheck = givesCheck;
        this.earlyRegret = earlyRegret;
        this.earlyStrength = earlyStrength;
        this.finalStrength = finalStrength;
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

    public ExtraordinaryReason getExtraordinaryReason() {
        return extraordinaryReason;
    }

    public Double getMaterialInvestment() {
        return materialInvestment;
    }

    public MaterialSacrificeType getSacrificeType() {
        return sacrificeType;
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

    public Double getEarlyStrength() {
        return earlyStrength;
    }

    public Double getFinalStrength() {
        return finalStrength;
    }
}
