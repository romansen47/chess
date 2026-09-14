package demo.chess.notation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.save.GameSaver;

class PgnAnnotationParserTest {

    @Test
    void parsesCommentsNagsEvaluationsAndVariations() throws Exception {
        String pgn = """
                [Event "Annotations"]
                [Result "*"]

                1. e4 $1 {Central control. [%eval 0.35]} e5
                2. Nf3?! (2. Bc4 Nc6) Nc6 *
                """;

        Map<Integer, PgnMoveAnnotation> annotations = new PgnAnnotationParser().parse(pgn);

        assertEquals("!", annotations.get(1).nag());
        assertEquals("Central control.", annotations.get(1).comment());
        assertEquals("0.35", annotations.get(1).evaluation());
        assertEquals("?!", annotations.get(3).nag());
        assertEquals(1, annotations.get(3).variations().size());
        assertTrue(annotations.get(3).variations().get(0).contains("2. Bc4"));
    }

    @Test
    public void annotationExportRoundTripsThroughStandardPgn() throws Exception {
        List<String> moves = List.of("e2e4", "e7e5", "g1f3", "b8c6");
        Simulation simulation = Simulation.createSimulation();
        new GameLoader().loadGame(moves, simulation);

        Map<Integer, PgnMoveAnnotation> annotations = new LinkedHashMap<>();
        annotations.put(1, new PgnMoveAnnotation(
                "!",
                "Central control.",
                "0.35",
                List.of()));
        annotations.put(3, new PgnMoveAnnotation(
                "?!",
                "A playable but debatable choice.",
                null,
                List.of("2. Bc4 Nc6")));

        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Event", "Round trip");
        tags.put("White", "White");
        tags.put("Black", "Black");
        tags.put("Result", "*");

        String exported = new GameSaver().toPgn(
                simulation.getMoveList(),
                tags,
                annotations);

        Map<Integer, PgnMoveAnnotation> reparsed =
                new PgnAnnotationParser().parse(exported);

        assertEquals("!", reparsed.get(1).nag());
        assertEquals("Central control.", reparsed.get(1).comment());
        assertEquals("0.35", reparsed.get(1).evaluation());
        assertEquals("?!", reparsed.get(3).nag());
        assertEquals("A playable but debatable choice.", reparsed.get(3).comment());
        assertEquals(List.of("2. Bc4 Nc6"), reparsed.get(3).variations());
    }

}
