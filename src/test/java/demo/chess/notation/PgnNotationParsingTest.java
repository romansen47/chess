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
                        "e8h8"),
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

    /**
     * Verifies that a lowercase b-file prefix remains a pawn source file and is
     * not mistaken for the uppercase SAN bishop designator.
     */
    @Test
    public void parsesBFilePawnCapture() throws Exception {
        GameLoader loader = new GameLoader();

        String pgn = """
                [Event "B-file Pawn Capture"]
                [Site "?"]
                [Date "2026.09.06"]
                [Round "3"]
                [White "White"]
                [Black "Black"]
                [Result "*"]

                1. b4 c5 2. bxc5 *
                """;

        assertEquals(
                List.of("b2b4", "c7c5", "b4c5"),
                loader.parsePgnMoveList(pgn));
    }
    /**
     * Verifies that pseudo-legal dummy moves do not create false SAN ambiguity
     * when one candidate is illegal because it exposes the king.
     */
    @Test
    public void parsesPinnedKnightWithoutFalseSanAmbiguity() throws Exception {
        GameLoader loader = new GameLoader();

        String pgn = """
                [Event "Yugoslavia ct  Rd: 3"]
                [Site "Yugoslavia ct  Rd: 3"]
                [Date "1959.??.??"]
                [Round "?"]
                [White "Mikhail Tal"]
                [Black "Robert James Fischer"]
                [Result "1-0"]
                [ECO "E93"]

                1. d4 Nf6 2. c4 g6 3. Nc3 Bg7 4. e4 d6 5. Be2 O-O 6. Nf3 e5
                7. d5 Nbd7 8. Bg5 h6 9. Bh4 a6 10. O-O Qe8 11. Nd2 Nh7
                12. b4 Bf6 13. Bxf6 Nhxf6 14. Nb3 Qe7 15. Qd2 Kh7 16. Qe3 Ng8
                17. c5 f5 18. exf5 gxf5 19. f4 exf4 20. Qxf4 dxc5 21. Bd3 cxb4
                22. Rae1 Qf6 23. Re6 Qxc3 24. Bxf5+ Rxf5 25. Qxf5+ Kh8
                26. Rf3 Qb2 27. Re8 Nf6 28. Qxf6+ Qxf6 29. Rxf6 Kg7
                30. Rff8 Ne7 31. Na5 h5 32. h4 Rb8 33. Nc4 b5 34. Ne5 1-0
                """;

        List<String> moves = loader.parsePgnMoveList(pgn);

        assertEquals(67, moves.size());
        assertEquals("d7f6", moves.get(53));
        assertEquals("c4e5", moves.get(66));
    }

}
