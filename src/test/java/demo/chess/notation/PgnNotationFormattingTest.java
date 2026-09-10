package demo.chess.notation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.DummyGame;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.save.GameSaver;

/**
 * Regression tests for canonical SAN formatting.
 */
public class PgnNotationFormattingTest {

    private final GameLoader gameLoader = new GameLoader();

    /**
     * Verifies normal moves, captures, checks, mate, castling, en passant and promotion.
     */
    @Test
    public void formatsCommonSanVariants() throws Exception {
        assertNotation(List.of(), "e2e4", "e4", "e4");
        assertNotation(List.of("e2e4", "d7d5"), "e4d5", "exd5", "exd5");
        assertNotation(
                List.of("e2e4", "e7e5", "g1f3", "d7d6"),
                "f1b5",
                "Bb5+",
                "♗b5+");
        assertNotation(
                List.of("e2e4", "e7e5", "f1c4", "b8c6", "g1f3", "g8f6"),
                "c4f7",
                "Bxf7+",
                "♗xf7+");
        assertNotation(
                List.of("f2f3", "e7e5", "g2g4"),
                "d8h4",
                "Qh4#",
                "♛h4#");
        assertNotation(
                List.of("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5"),
                "e1g1",
                "O-O",
                "0-0");
        assertNotation(
                List.of("e2e4", "d7d5", "e4e5", "f7f5"),
                "e5f6",
                "exf6",
                "exf6 e.p.");

        // The rook promotion on h8 is mate: the rook attacks e8 through g8/f8,
        // while the black king is boxed in by its own pieces and cannot block or capture.
        assertNotation(
                List.of("h2h4", "g7g5", "h4g5", "g8h6", "g5h6", "f8g7", "h6g7", "b8c6"),
                "g7h8r",
                "gxh8=R#",
                "gxh8=R#");
    }

    /**
     * A pseudo-legal competing knight is pinned and must not force SAN disambiguation.
     */
    @Test
    public void dummyFormattingEscalatesDisambiguationWhenLegalityMatters() throws Exception {
        List<String> history = List.of(
                "d2d3", "e7e6",
                "b1c3", "f8b4",
                "g1f3", "g8f6",
                "f3d4", "d7d6");

        Simulation simulation = Simulation.createSimulation();
        gameLoader.loadGame(history, simulation);
        Move legalMove = findMove(simulation, "d4b5");
        assertEquals("Nb5", PgnNotation.toSan(simulation, legalMove));

        DummyGame dummyGame = Simulation.createDummySimulation();
        gameLoader.loadGame(history, dummyGame);
        Move dummyMove = findMove(dummyGame, "d4b5");
        assertEquals("Nb5", PgnNotation.toSan(dummyGame, dummyMove));
    }

    /**
     * Verifies that PGN export emits suffixes and the existing fast parser can read them back.
     */
    @Test
    public void exportRoundTripPreservesCheckingAndMatingMoves() throws Exception {
        List<String> checkingGame = List.of("e2e4", "e7e5", "g1f3", "d7d6", "f1b5");
        String checkingPgn = export(checkingGame, "*");
        assertTrue(checkingPgn.contains("Bb5+"));
        assertEquals(checkingGame, gameLoader.parsePgnMoveList(checkingPgn));

        List<String> matingGame = List.of("f2f3", "e7e5", "g2g4", "d8h4");
        String matingPgn = export(matingGame, "0-1");
        assertTrue(matingPgn.contains("Qh4#"));
        assertEquals(matingGame, gameLoader.parsePgnMoveList(matingPgn));
    }

    private void assertNotation(
            List<String> history,
            String uciMove,
            String expectedSan,
            String expectedDisplay) throws Exception {
        Simulation simulation = Simulation.createSimulation();
        gameLoader.loadGame(history, simulation);
        Move simulationMove = findMove(simulation, uciMove);
        assertEquals(expectedSan, PgnNotation.toSan(simulation, simulationMove));
        assertEquals(expectedDisplay, PgnNotation.toDisplayNotation(simulation, simulationMove));

        DummyGame dummyGame = Simulation.createDummySimulation();
        gameLoader.loadGame(history, dummyGame);
        Move dummyMove = findMove(dummyGame, uciMove);
        assertEquals(expectedSan, PgnNotation.toSan(dummyGame, dummyMove));
        assertEquals(expectedDisplay, PgnNotation.toDisplayNotation(dummyGame, dummyMove));
    }

    private Move findMove(Game game, String uciMove) throws Exception {
        for (Move move : game.getPlayer().getValidMoves(game)) {
            if (uciMove.equalsIgnoreCase(move.toString())) {
                return move;
            }
        }
        throw new NoMoveFoundException("No move found in test position: " + uciMove);
    }

    private String export(List<String> uciMoves, String result) throws Exception {
        Simulation simulation = Simulation.createSimulation();
        gameLoader.loadGame(uciMoves, simulation);

        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Event", "SAN Formatting Test");
        tags.put("Result", result);

        return new GameSaver().toPgn(new ArrayList<>(simulation.getMoveList()), tags);
    }
}
