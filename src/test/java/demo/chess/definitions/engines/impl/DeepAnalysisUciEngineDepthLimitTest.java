package demo.chess.definitions.engines.impl;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import demo.chess.definitions.engines.uci.UciInfoLine;

public class DeepAnalysisUciEngineDepthLimitTest {

    @Test
    public void fixedDepthRejectsHelperThreadIterationsBeyondRequestedDepth() {
        List<UciInfoLine> infoLines = List.of(
                cp(7, 1, 10),
                cp(8, 1, 12),
                cp(8, 2, 8),
                cp(10, 1, 15),
                cp(14, 1, 20));

        assertEquals(
                List.of(
                        cp(7, 1, 10),
                        cp(8, 1, 12),
                        cp(8, 2, 8)),
                DeepAnalysisUciEngine.capInfoLinesAtRequestedDepth(
                        infoLines,
                        8));
    }

    @Test
    public void timeBasedSearchKeepsAllReportedDepths() {
        List<UciInfoLine> infoLines = List.of(
                cp(8, 1, 12),
                cp(13, 1, 18));

        assertEquals(
                infoLines,
                DeepAnalysisUciEngine.capInfoLinesAtRequestedDepth(
                        infoLines,
                        0));
    }

    private static UciInfoLine cp(
            int depth,
            int multiPv,
            int centipawns) {
        return new UciInfoLine(
                depth,
                multiPv,
                centipawns,
                null,
                "e2e4 e7e5");
    }
}
