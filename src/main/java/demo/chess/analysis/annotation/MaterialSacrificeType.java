package demo.chess.analysis.annotation;

/**
 * Describes why material is deliberately invested by a candidate move.
 */
public enum MaterialSacrificeType {

    /**
     * The piece moved by the candidate is itself captured in the principal
     * variation and the loss is not immediately recovered.
     */
    ACTIVE_INVESTMENT,

    /**
     * The candidate newly exposes another piece to a legal material-winning
     * capture.
     */
    NEW_MATERIAL_OFFER,

    /**
     * A piece was already threatened, had a legal way to get out of danger,
     * but the candidate deliberately plays something else and leaves it
     * available.
     */
    DECLINED_MATERIAL_SAVE
}
