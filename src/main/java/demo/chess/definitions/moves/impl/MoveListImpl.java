package demo.chess.definitions.moves.impl;

import java.util.ArrayList;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;

/** Implementation of a move list including the game's initial position. */
public class MoveListImpl extends ArrayList<Move> implements MoveList {

    private static final long serialVersionUID = 1L;

    private ChessStartingPosition startingPosition = ChessStartingPosition.STANDARD;

    @Override
    public ChessStartingPosition getStartingPosition() {
        return startingPosition;
    }

    @Override
    public void setStartingPosition(ChessStartingPosition startingPosition) {
        this.startingPosition = startingPosition != null
                ? startingPosition
                : ChessStartingPosition.STANDARD;
    }
}
