package demo.chess.definitions.engines.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import demo.chess.definitions.engines.uci.UciInfoLine;
import demo.chess.definitions.engines.uci.UciInfoParser;

public class EvaluationUciEngineInfoLineTest {

    @Test
    public void berserkStatusLineWithDepthIsNotPrincipalVariation() {
        String line =
                "info string time -1 start 123456 alloc 2147483647 max 2147483647 "
                + "depth 127 timeset 0 searchmoves 0";

        assertTrue(UciInfoParser.parsePrincipalVariation(line).isEmpty());
    }

    @Test
    public void berserkCurrmoveProgressLineIsNotPrincipalVariation() {
        String line = "info depth 54 currmove f8d8 currmovenumber 26";

        assertTrue(UciInfoParser.parsePrincipalVariation(line).isEmpty());
    }

    @Test
    public void berserkMultiPvLineIsParsedStructurally() {
        String line =
                "info depth 54 seldepth 54 multipv 2 score mate -14 wdl 0 0 1000 "
                + "nodes 498352908 nps 11856793 hashfull 1000 tbhits 0 time 42031 "
                + "pv f8g8 e7g8 c1g8";

        UciInfoLine parsed = UciInfoParser.parsePrincipalVariation(line).orElseThrow();

        assertEquals(54, parsed.depth());
        assertEquals(2, parsed.multiPv());
        assertEquals(Integer.valueOf(-14), parsed.mateScore());
        assertEquals("f8g8 e7g8 c1g8", parsed.pv());
    }

    @Test
    public void centipawnSinglePvUsesDefaultVariantOne() {
        String line =
                "info depth 12 seldepth 18 score cp 34 nodes 123456 nps 1000000 "
                + "pv e2e4 e7e5 g1f3";

        UciInfoLine parsed = UciInfoParser.parsePrincipalVariation(line).orElseThrow();

        assertEquals(12, parsed.depth());
        assertEquals(1, parsed.multiPv());
        assertEquals(Integer.valueOf(34), parsed.centipawnScore());
        assertFalse(parsed.isMateScore());
    }

    @Test
    public void malformedNumericFieldsAreIgnored() {
        assertTrue(UciInfoParser.parsePrincipalVariation(
                "info depth many score cp 20 pv e2e4").isEmpty());
    }
}
