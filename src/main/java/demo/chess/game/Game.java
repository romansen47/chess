package demo.chess.game;

import java.io.IOException;
import java.util.List;

import demo.chess.definitions.CastlingRights;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;

/** Interface representing the core functionalities of a chess game. */
public interface Game {

    Board getChessBoard();
    MoveList getMoveList();
    Player getWhitePlayer();
    Player getBlackPlayer();
    Player getPlayer();
    void setPlayer(Player player);
    void createPieces();
    void switchPlayer();
    void apply(Move move) throws NoMoveFoundException, IOException;
    void setState(State stalemate);
    State getState();
    int getTimeForEachPlayer();
    int getIncrementForWhite();
    void setIncrementForWhite(int incrementForWhite);
    int getIncrementForBlack();
    void setIncrementForBlack(int incrementForBlack);

    /** Initial Chess960 position. Classical chess is position 518. */
    ChessStartingPosition getStartingPosition();

    /** Current castling rights, including the original rook files. */
    CastlingRights getCastlingRights();

    default void configureTimeControl(
            int whiteIncrementSeconds,
            int blackIncrementSeconds,
            int additionalTimeAfter40MovesSeconds) {
        setIncrementForWhite(Math.max(0, whiteIncrementSeconds));
        setIncrementForBlack(Math.max(0, blackIncrementSeconds));
        int additionalTimeSeconds = Math.max(0, additionalTimeAfter40MovesSeconds);
        getWhitePlayer().setAdditionalTime(additionalTimeSeconds);
        getBlackPlayer().setAdditionalTime(additionalTimeSeconds);
    }

    default Color getTimedOutColor() {
        return null;
    }

    List<String> getSanMoveList();
    void setSanMoveList(List<String> sanMoveList);
}
