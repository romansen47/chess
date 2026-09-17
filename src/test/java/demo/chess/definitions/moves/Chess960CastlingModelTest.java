package demo.chess.definitions.moves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.impl.CastlingImpl;
import demo.chess.definitions.moves.impl.MoveListImpl;
import demo.chess.definitions.pieces.impl.King;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.UciMoveCodec;
import demo.chess.save.GameSaver;

public class Chess960CastlingModelTest {

    @Test
    public void standardCastlingKeepsClassicalUciNotation() {
        Simulation game = Simulation.createSimulation(ChessStartingPosition.STANDARD);
        King king = (King) game.getChessBoard().getField(5, 1).getPiece();
        Rook rook = (Rook) game.getChessBoard().getField(8, 1).getPiece();
        Castling castling = new CastlingImpl(king, rook);

        assertEquals("e1g1", castling.toString());
        assertSame(game.getChessBoard().getField(8, 1), castling.getRookSource());
        assertEquals("e1g1", UciMoveCodec.encode(game, castling));
    }

    @Test
    public void chess960SupportsKingAlreadyOnFinalSquare() {
        Simulation game = Simulation.createSimulation(ChessStartingPosition.of(0));
        King king = (King) game.getChessBoard().getField(7, 1).getPiece();
        Rook rook = (Rook) game.getChessBoard().getField(8, 1).getPiece();
        Castling castling = new CastlingImpl(king, rook);

        assertSame(game.getChessBoard().getField(7, 1), castling.getKingTarget());
        assertSame(game.getChessBoard().getField(8, 1), castling.getRookSource());
        assertSame(game.getChessBoard().getField(6, 1), castling.getRookTarget());
        assertEquals("g1g1", castling.toString());
        assertEquals("g1h1", UciMoveCodec.encode(game, castling));
    }

    @Test
    public void rawUciExportUsesCodecInsteadOfMoveToString() {
        Simulation game = Simulation.createSimulation(ChessStartingPosition.of(0));
        King king = (King) game.getChessBoard().getField(7, 1).getPiece();
        Rook rook = (Rook) game.getChessBoard().getField(8, 1).getPiece();
        Castling castling = new CastlingImpl(king, rook);

        MoveListImpl moves = new MoveListImpl();
        moves.setStartingPosition(ChessStartingPosition.of(0));
        moves.add(castling);

        assertEquals("g1h1\n", new GameSaver().toUci(moves));
    }
}
