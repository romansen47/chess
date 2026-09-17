package demo.chess.definitions.moves;

import demo.chess.definitions.CastlingSide;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.pieces.impl.Rook;

/**
 * A castling move expressed in domain terms.
 *
 * <p>Chess960 castling cannot be described reliably by only the king source
 * and a generic target square: the king and rook can each already occupy their
 * final square, or they can move by unusual distances. Therefore a castling
 * move exposes both piece trajectories explicitly.</p>
 *
 * <p>For backward compatibility with the historical {@link Move} model,
 * {@link #getTarget()} still represents the rook's original square. New code
 * should use {@link #getKingTarget()}, {@link #getRookSource()} and
 * {@link #getRookTarget()} when reasoning about castling geometry.</p>
 */
public interface Castling extends Move {

    /** @return rook participating in this castling move */
    Rook getRook();

    /** @return king-side or queen-side castling independent of initial files */
    CastlingSide getSide();

    /** @return original rook square before castling */
    Field getRookSource();

    /** @return final king square (g-file king-side, c-file queen-side) */
    Field getKingTarget();

    /** @return final rook square (f-file king-side, d-file queen-side) */
    Field getRookTarget();
}
