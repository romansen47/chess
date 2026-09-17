package demo.chess.definitions.moves;

import java.util.List;

import demo.chess.definitions.ChessStartingPosition;

/**
 * Interface representing a list of moves in a chess game.
 *
 * <p>The initial position belongs to the move history because replay and
 * analysis cannot reconstruct a Chess960 game from the moves alone.</p>
 */
public interface MoveList extends List<Move> {

    default ChessStartingPosition getStartingPosition() {
        return ChessStartingPosition.STANDARD;
    }

    default void setStartingPosition(ChessStartingPosition startingPosition) {
        // Compatibility default for foreign MoveList implementations.
    }
}
