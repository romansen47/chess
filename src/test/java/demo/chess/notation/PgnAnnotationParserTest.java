package demo.chess.notation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

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
}
