package demo.chess.definitions.moves;

import demo.chess.definitions.CastlingSide;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.pieces.impl.Rook;

/** Interface representing a castling move in a chess game. */
public interface Castling extends Move {
    Rook getRook();
    CastlingSide getSide();
    Field getKingTarget();
    Field getRookTarget();
}
