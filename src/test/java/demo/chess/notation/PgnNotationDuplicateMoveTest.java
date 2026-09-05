package demo.chess.notation;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import demo.chess.load.GameLoader;

/**
 * Regression coverage for duplicate legal-move objects returned by the move generator.
 */
public class PgnNotationDuplicateMoveTest {

    /**
     * Verifies that equivalent castling candidates do not make SAN ambiguous.
     */
    @Test
    public void acceptsEquivalentCastlingCandidates() throws Exception {
        GameLoader loader = new GameLoader();
        String pgn = """
                [Event "Castling Test"]
                [Site "?"]
                [Date "2026.09.06"]
                [Round "1"]
                [White "White"]
                [Black "Black"]
                [Result "*"]

                1. Nf3 Nf6 2. g3 g6 3. Bg2 Bg7 4. O-O O-O *
                """;

        assertEquals(
                List.of(
                        "g1f3",
                        "g8f6",
                        "g2g3",
                        "g7g6",
                        "f1g2",
                        "f8g7",
                        "e1g1",
                        "e8g8"),
                loader.parsePgnMoveList(pgn));
    }
}
