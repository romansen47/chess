package demo.chess.definitions.engines.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EvaluationUciEngineInfoLineTest {

    @Test
    public void berserkStatusLineWithDepthIsNotPrincipalVariation() {
        String line =
                "info string time -1 start 123456 alloc 2147483647 max 2147483647 "
                + "depth 127 timeset 0 searchmoves 0";

        assertFalse(EvaluationUciEngine.isPrincipalVariationInfoLine(line));
    }

    @Test
    public void berserkCurrmoveProgressLineIsNotPrincipalVariation() {
        String line = "info depth 54 currmove f8d8 currmovenumber 26";

        assertFalse(EvaluationUciEngine.isPrincipalVariationInfoLine(line));
    }

    @Test
    public void berserkMultiPvLineIsPrincipalVariation() {
        String line =
                "info depth 54 seldepth 54 multipv 1 score mate -53 wdl 0 0 1000 "
                + "nodes 498352908 nps 11856793 hashfull 1000 tbhits 0 time 42031 "
                + "pv f8g8 e7g8 c1g8";

        assertTrue(EvaluationUciEngine.isPrincipalVariationInfoLine(line));
    }

    @Test
    public void singlePvLineWithoutMultiPvIsPrincipalVariation() {
        String line =
                "info depth 12 seldepth 18 score cp 34 nodes 123456 nps 1000000 "
                + "pv e2e4 e7e5 g1f3";

        assertTrue(EvaluationUciEngine.isPrincipalVariationInfoLine(line));
    }
}
