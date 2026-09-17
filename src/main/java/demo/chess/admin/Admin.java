package demo.chess.admin;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.game.Game;

/** Interface representing administrative functionalities for a chess game. */
public interface Admin {

    Game chessGame(int time);

    default Game chessGame(int time, ChessStartingPosition startingPosition) {
        if (startingPosition == null || startingPosition.isStandard()) {
            return chessGame(time);
        }
        throw new UnsupportedOperationException("Admin does not support Chess960 starting positions");
    }
}
