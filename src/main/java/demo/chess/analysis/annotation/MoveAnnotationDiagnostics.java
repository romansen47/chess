package demo.chess.analysis.annotation;

/**
 * Diagnostic-only explanation of the deep-discovery part of extraordinary
 * move detection. It is intentionally separate from {@link MoveAnnotation} so
 * rejected "!!" candidates remain observable without becoming annotations.
 */
public final class MoveAnnotationDiagnostics {

    private final boolean extraordinaryCandidate;
    private final DeepDiscoveryRejectionReason deepDiscoveryRejectedBy;
    private final int earlyWindowStartDepth;
    private final int earlyWindowEndDepth;
    private final int earlySampleCount;
    private final Integer earlyDepth;
    private final Integer earlyRank;
    private final int finalDepth;
    private final int finalRank;
    private final double finalRegret;
    private final Double earlyRegret;
    private final Double earlyStrength;
    private final double finalStrength;
    private final Double strengthGain;

    public MoveAnnotationDiagnostics(
            boolean extraordinaryCandidate,
            DeepDiscoveryRejectionReason deepDiscoveryRejectedBy,
            int earlyWindowStartDepth,
            int earlyWindowEndDepth,
            int earlySampleCount,
            Integer earlyDepth,
            Integer earlyRank,
            int finalDepth,
            int finalRank,
            double finalRegret,
            Double earlyRegret,
            Double earlyStrength,
            double finalStrength,
            Double strengthGain) {
        this.extraordinaryCandidate = extraordinaryCandidate;
        this.deepDiscoveryRejectedBy = deepDiscoveryRejectedBy;
        this.earlyWindowStartDepth = earlyWindowStartDepth;
        this.earlyWindowEndDepth = earlyWindowEndDepth;
        this.earlySampleCount = earlySampleCount;
        this.earlyDepth = earlyDepth;
        this.earlyRank = earlyRank;
        this.finalDepth = finalDepth;
        this.finalRank = finalRank;
        this.finalRegret = finalRegret;
        this.earlyRegret = earlyRegret;
        this.earlyStrength = earlyStrength;
        this.finalStrength = finalStrength;
        this.strengthGain = strengthGain;
    }

    public boolean isExtraordinaryCandidate() {
        return extraordinaryCandidate;
    }

    public DeepDiscoveryRejectionReason getDeepDiscoveryRejectedBy() {
        return deepDiscoveryRejectedBy;
    }

    public int getEarlyWindowStartDepth() {
        return earlyWindowStartDepth;
    }

    public int getEarlyWindowEndDepth() {
        return earlyWindowEndDepth;
    }

    public int getEarlySampleCount() {
        return earlySampleCount;
    }

    public Integer getEarlyDepth() {
        return earlyDepth;
    }

    public Integer getEarlyRank() {
        return earlyRank;
    }

    public int getFinalDepth() {
        return finalDepth;
    }

    public int getFinalRank() {
        return finalRank;
    }

    public double getFinalRegret() {
        return finalRegret;
    }

    public Double getEarlyRegret() {
        return earlyRegret;
    }

    public Double getEarlyStrength() {
        return earlyStrength;
    }

    public double getFinalStrength() {
        return finalStrength;
    }

    public Double getStrengthGain() {
        return strengthGain;
    }
}
