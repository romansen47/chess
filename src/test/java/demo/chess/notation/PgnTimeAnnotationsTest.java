package demo.chess.notation;

import static org.junit.Assert.*;

import org.junit.Test;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.save.GameSaver;

public class PgnTimeAnnotationsTest {
    @Test
    public void clocksCommentsAndPreambleSurviveRoundTrip() throws Exception {
        String pgn = """
                [Result "*"]

                {Introduction.}
                1. e4 {Plan. [%clk 0:05:02.125] [%emt 0:00:01.5] [%eval 0.20]} e5
                {Black's reply.
                [%clk 0:04:59]
                [Event "this is a comment"]
                } 2. Nf3 {[%clk 0:00:00]} Nc6 *
                """;
        var parser = new PgnAnnotationParser();
        var annotations = parser.parse(pgn);
        assertEquals("Introduction.", annotations.get(0).comment());
        assertEquals(Long.valueOf(302125), annotations.get(1).clockMillis());
        assertEquals(Long.valueOf(1500), annotations.get(1).elapsedMoveMillis());
        assertEquals("Plan.", annotations.get(1).comment());
        assertEquals(Long.valueOf(0), annotations.get(3).clockMillis());
        assertFalse(annotations.get(3).isEmpty());
        assertTrue(annotations.get(2).comment().contains("[Event"));
        var loader = new GameLoader();
        var game = Simulation.createSimulation();
        loader.loadGame(loader.parsePgnMoveList(pgn), game);
        var exported = new GameSaver().toPgn(game.getMoveList(), loader.parsePgnTags(pgn), annotations);
        assertEquals(annotations, parser.parse(exported));
    }

    @Test
    public void invalidOrUnknownTagsRemainText() throws Exception {
        var annotations = new PgnAnnotationParser().parse(
                "1. e4 {[%clk broken] [%emt 0:60:00] [%foo x] [%clk 999999999999999999999:00:00]} e5 *");
        assertNull(annotations.get(1).clockMillis());
        assertNull(annotations.get(1).elapsedMoveMillis());
        assertTrue(annotations.get(1).comment().contains("[%clk broken]"));
        assertTrue(annotations.get(1).comment().contains("[%foo x]"));
    }

    @Test
    public void enrichesOnlyMissingFieldsAndIsIdempotent() throws Exception {
        String original = "1. e4 {My note. [%clk 0:05:00] [%eval 0.2]} e5 *";
        String incoming = "1. e4 {Other note. [%clk 0:04:00] [%emt 0:00:03] [%eval 0.6]} e5 {Reply.} *";
        var merger = new PgnAnnotationMerger();
        String merged = merger.fillMissing(original, incoming);
        var annotations = new PgnAnnotationParser().parse(merged);
        assertEquals("My note.", annotations.get(1).comment());
        assertEquals(Long.valueOf(300000), annotations.get(1).clockMillis());
        assertEquals(Long.valueOf(3000), annotations.get(1).elapsedMoveMillis());
        assertEquals("0.2", annotations.get(1).evaluation());
        assertEquals("Reply.", annotations.get(2).comment());
        assertEquals(merged, merger.fillMissing(merged, incoming));
    }
}
