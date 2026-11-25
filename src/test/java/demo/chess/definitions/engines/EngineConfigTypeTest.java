package demo.chess.definitions.engines;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

class EngineConfigTypeTest {

    @Test
    void readsLegacyDeepAnalysisValueAsGameAnalysis() {
        assertEquals(
                EngineConfigType.GAME_ANALYSIS,
                EngineConfigType.fromValue("DEEP_ANALYSIS"));
    }
}
