package demo.chess.definitions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChessStartingPositionTest {

    @Test
    public void standardPositionIs518() {
        ChessStartingPosition position = ChessStartingPosition.of(518);
        assertTrue(position.isStandard());
        assertEquals("RNBQKBNR", backRank(position));
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", position.initialFen());
    }

    @Test
    public void decodesBoundaryPositions() {
        assertEquals("BBQNNRKR", backRank(ChessStartingPosition.of(0)));
        assertEquals("RKRNNQBB", backRank(ChessStartingPosition.of(959)));
    }

    @Test
    public void roundTripsInitialFen() {
        for (int id : new int[] {0, 3, 42, 518, 537, 959}) {
            ChessStartingPosition original = ChessStartingPosition.of(id);
            assertEquals(id, ChessStartingPosition.fromInitialFen(original.initialFen()).getId());
        }
    }

    private String backRank(ChessStartingPosition position) {
        StringBuilder result = new StringBuilder();
        for (PieceType type : position.getBackRank()) {
            result.append(switch (type) {
                case ROOK -> 'R';
                case KNIGHT -> 'N';
                case BISHOP -> 'B';
                case QUEEN -> 'Q';
                case KING -> 'K';
                case PAWN -> 'P';
            });
        }
        return result.toString();
    }
}
