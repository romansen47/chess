package demo.chess.game.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import demo.chess.admin.impl.ChessAdmin;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.impl.ChessBoard;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.impl.MoveListImpl;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.players.impl.BlackPlayerImpl;
import demo.chess.definitions.players.impl.WhitePlayerImpl;
import demo.chess.definitions.states.State;

/**
 * Integration coverage for live-game insufficient material handling.
 */
public class ChessGameInsufficientMaterialTest {

    @Test
    public void liveGameEndsImmediatelyWithBareKings() throws Exception {
        MoveList moveList = new MoveListImpl();
        ChessGame game = new ChessGame(
                new ChessBoard(),
                new WhitePlayerImpl(moveList, "White"),
                new BlackPlayerImpl(moveList, "Black"),
                moveList,
                new ChessAdmin(),
                60);

        removeNonKingPieces(game.getWhitePlayer());
        removeNonKingPieces(game.getBlackPlayer());

        assertTrue(game.checkForGameEnd());
        assertEquals(State.DRAW_BY_INSUFFICIENT_MATERIAL, game.getState());
    }

    private void removeNonKingPieces(Player player) {
        List<Piece> piecesToRemove = new ArrayList<>();
        for (Piece piece : player.getPieces()) {
            if (piece.getType() != PieceType.KING) {
                piecesToRemove.add(piece);
            }
        }

        for (Piece piece : piecesToRemove) {
            if (piece.getField() != null) {
                piece.getField().setPiece(null);
            }
            player.getPieces().remove(piece);
        }
    }
}
