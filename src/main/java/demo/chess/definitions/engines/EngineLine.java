package demo.chess.definitions.engines;

/**
 * Eine von der Engine gelieferte Hauptvariante.
 *
 * evaluation ist aus Sicht von Weiß normalisiert. Bei Matt wird weiterhin der
 * bisherige +/-99-Wert für die bestehende Bewertungs-/Balkenlogik verwendet.
 * mateDistance enthält zusätzlich die von UCI gemeldete Mattdistanz als
 * absolute Zuganzahl; null bedeutet, dass die Engine einen normalen cp-Score
 * geliefert hat.
 */
public class EngineLine {

    private final double evaluation;
    private final int depth;
    private final Integer mateDistance;
    private final String moves;

    /**
     * Creates a new EngineLine instance.
     * @param evaluation the evaluation
     * @param depth the depth
     * @param mateDistance the mate distance
     * @param moves the moves
     */
    public EngineLine(double evaluation, int depth, Integer mateDistance, String moves) {
        this.evaluation = evaluation;
        this.depth = depth;
        this.mateDistance = mateDistance;
        this.moves = moves;
    }

    /**
     * Returns the evaluation.
     * @return the evaluation
     */
    public double getEvaluation() {
        return evaluation;
    }

    /**
     * Returns the depth.
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Returns the mate distance.
     * @return the mate distance
     */
    public Integer getMateDistance() {
        return mateDistance;
    }

    /**
     * Returns the moves.
     * @return the moves
     */
    public String getMoves() {
        return moves;
    }
}
