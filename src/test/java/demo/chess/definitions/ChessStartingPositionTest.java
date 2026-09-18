package demo.chess.definitions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ChessStartingPositionTest {

    @Test
    public void standardPositionIs518() {
        ChessStartingPosition position = ChessStartingPosition.of(518);
        assertSame(ChessStartingPosition.STANDARD, position);
        assertTrue(position.isStandard());
        assertEquals("RNBQKBNR", backRank(position));
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w HAha - 0 1", position.initialFen());
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
            assertSame(original, ChessStartingPosition.fromInitialFen(original.initialFen()));
        }
    }

    @Test
    public void all960PositionsAreCanonicalUniqueAndLegal() {
        Set<String> layouts = new HashSet<>();
        for (int id = ChessStartingPosition.MIN_ID; id <= ChessStartingPosition.MAX_ID; id++) {
            ChessStartingPosition position = ChessStartingPosition.of(id);
            assertSame(position, ChessStartingPosition.of(id));
            assertTrue(position.getQueenSideRookFile() < position.getKingFile());
            assertTrue(position.getKingFile() < position.getKingSideRookFile());
            assertTrue(oppositeSquareColors(position));
            assertTrue("duplicate layout for id " + id, layouts.add(backRank(position)));
        }
        assertEquals(960, layouts.size());
    }

    private boolean oppositeSquareColors(ChessStartingPosition position) {
        int firstBishopFile = -1;
        int secondBishopFile = -1;
        for (int file = 1; file <= 8; file++) {
            if (position.getPieceTypeAtFile(file) == PieceType.BISHOP) {
                if (firstBishopFile < 0) firstBishopFile = file;
                else secondBishopFile = file;
            }
        }
        return firstBishopFile % 2 != secondBishopFile % 2;
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
