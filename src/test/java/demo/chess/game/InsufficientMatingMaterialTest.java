package demo.chess.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Bishop;
import demo.chess.definitions.pieces.impl.Knight;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;
import demo.chess.game.impl.Simulation;

/**
 * Regression tests for insufficient mating material detection.
 */
public class InsufficientMatingMaterialTest {

    @Test
    public void bareKingsAreDrawn() {
        Simulation game = strippedSimulation();

        assertTrue(TerminalPositionEvaluator.hasInsufficientMatingMaterial(game));
        assertEquals(
                State.DRAW_BY_INSUFFICIENT_MATERIAL,
                TerminalPositionEvaluator.determineState(game));
    }

    @Test
    public void singleBishopAgainstKingIsDrawn() {
        Simulation game = strippedSimulation();
        addBishop(game, Color.WHITE, 3, 1);

        assertTrue(TerminalPositionEvaluator.hasInsufficientMatingMaterial(game));
        assertEquals(
                State.DRAW_BY_INSUFFICIENT_MATERIAL,
                TerminalPositionEvaluator.determineState(game));
    }

    @Test
    public void singleKnightAgainstKingIsDrawn() {
        Simulation game = strippedSimulation();
        addKnight(game, Color.BLACK, 2, 8);

        assertTrue(TerminalPositionEvaluator.hasInsufficientMatingMaterial(game));
        assertEquals(
                State.DRAW_BY_INSUFFICIENT_MATERIAL,
                TerminalPositionEvaluator.determineState(game));
    }

    @Test
    public void bishopsConfinedToSameSquareColorAreDrawn() {
        Simulation game = strippedSimulation();
        addBishop(game, Color.WHITE, 3, 1);
        addBishop(game, Color.BLACK, 6, 8);

        assertTrue(TerminalPositionEvaluator.hasInsufficientMatingMaterial(game));
    }

    @Test
    public void oppositeColoredBishopsAreNotAutomaticallyDrawn() {
        Simulation game = strippedSimulation();
        addBishop(game, Color.WHITE, 3, 1);
        addBishop(game, Color.BLACK, 3, 8);

        assertFalse(TerminalPositionEvaluator.hasInsufficientMatingMaterial(game));
    }

    @Test
    public void twoKnightsAgainstKingAreNotAutomaticallyDrawn() {
        Simulation game = strippedSimulation();
        addKnight(game, Color.WHITE, 2, 1);
        addKnight(game, Color.WHITE, 7, 1);

        assertFalse(TerminalPositionEvaluator.hasInsufficientMatingMaterial(game));
    }

    @Test
    public void bishopAndKnightAgainstKingAreNotAutomaticallyDrawn() {
        Simulation game = strippedSimulation();
        addBishop(game, Color.WHITE, 3, 1);
        addKnight(game, Color.WHITE, 2, 1);

        assertFalse(TerminalPositionEvaluator.hasInsufficientMatingMaterial(game));
    }

    @Test
    public void rookAgainstKingIsNotAutomaticallyDrawn() {
        Simulation game = strippedSimulation();
        addRook(game, Color.WHITE, 1, 1);

        assertFalse(TerminalPositionEvaluator.hasInsufficientMatingMaterial(game));
    }

    private Simulation strippedSimulation() {
        Simulation game = Simulation.createSimulation();
        removeNonKingPieces(game.getWhitePlayer());
        removeNonKingPieces(game.getBlackPlayer());
        return game;
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

    private void addBishop(Simulation game, Color color, int file, int rank) {
        Bishop bishop = new Bishop(color, game.getChessBoard().getField(file, rank), game.getChessBoard(), true);
        addPiece(game, color, bishop);
    }

    private void addKnight(Simulation game, Color color, int file, int rank) {
        Knight knight = new Knight(color, game.getChessBoard().getField(file, rank), game.getChessBoard(), true);
        addPiece(game, color, knight);
    }

    private void addRook(Simulation game, Color color, int file, int rank) {
        Rook rook = new Rook(color, game.getChessBoard().getField(file, rank), game.getChessBoard(), true);
        addPiece(game, color, rook);
    }

    private void addPiece(Simulation game, Color color, Piece piece) {
        piece.setMoveList(game.getMoveList());
        Player player = color == Color.WHITE ? game.getWhitePlayer() : game.getBlackPlayer();
        player.getPieces().add(piece);
    }
}
