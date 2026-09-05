package demo.chess.notation;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import demo.chess.load.GameLoader;

/**
 * Regression tests for SAN parsing used by PGN imports.
 */
public class PgnNotationParsingTest {

    /**
     * Verifies common SAN, castling and source disambiguation in one PGN parser path.
     */
    @Test
    public void parsesCastlingAndSourceDisambiguation() throws Exception {
        GameLoader loader = new GameLoader();

        String pgn = """
                [Event "Parser Test"]
                [Site "?"]
                [Date "2026.09.06"]
                [Round "1"]
                [White "White"]
                [Black "Black"]
                [Result "*"]

                1. Nf3 Nf6 2. d4 d5 3. Nbd2 e6 4. e3 Be7 5. Bd3 O-O *
                """;

        assertEquals(
                List.of(
                        "g1f3",
                        "g8f6",
                        "d2d4",
                        "d7d5",
                        "b1d2",
                        "e7e6",
                        "e2e3",
                        "f8e7",
                        "f1d3",
                        "e8g8"),
                loader.parsePgnMoveList(pgn));
    }

    /**
     * Verifies captures and annotation suffixes without SAN reformatting.
     */
    @Test
    public void parsesCapturesAndAnnotations() throws Exception {
        GameLoader loader = new GameLoader();

        String pgn = """
                [Event "Parser Test"]
                [Site "?"]
                [Date "2026.09.06"]
                [Round "2"]
                [White "White"]
                [Black "Black"]
                [Result "1-0"]

                1. e4 d5 2. exd5 Qxd5 3. Nc3 Qd8 4. Nf3 Nf6 5. Bb5+ Bd7 1-0
                """;

        assertEquals(
                List.of(
                        "e2e4",
                        "d7d5",
                        "e4d5",
                        "d8d5",
                        "b1c3",
                        "d5d8",
                        "g1f3",
                        "g8f6",
                        "f1b5",
                        "c8d7"),
                loader.parsePgnMoveList(pgn));
    }
}
