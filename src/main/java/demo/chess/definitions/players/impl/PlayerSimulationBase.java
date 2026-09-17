package demo.chess.definitions.players.impl;

import java.io.IOException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

/** Shared simulation hook so concrete player variants can replace legality replay. */
abstract class PlayerSimulationBase {
    protected abstract boolean simulate(Game chessGame, Move move)
            throws NoMoveFoundException, IOException;
}
