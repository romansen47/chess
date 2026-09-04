package demo.chess.definitions.engines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class UciEngineDefinitionTest {

    /**
     * Verifies that profile values are resolved into an independent runtime configuration.
     */
    @Test
    public void createsIndependentRuntimeConfigFromProfileValues() {
        UciEngineDefinition definition = createDefinition();

        UciEngineConfig config = definition.createRuntimeConfig(
                12,
                3,
                Map.of(
                        "Hash", "128",
                        "Ponder", "false"));

        assertEquals("/usr/games/test-engine", config.getEngine());
        assertEquals("Test Engine", config.getEngineName());
        assertEquals("Test Author", config.getEngineAuthor());
        assertEquals(12, config.getDepth());
        assertEquals(3, config.getMoveTimeSeconds());
        assertEquals(128, config.getIntOption("hash", -1));
        assertEquals("false", config.getStringOption("PONDER", "missing"));
        assertEquals(
                "setoption name Hash value 128\nsetoption name Ponder value false",
                config.toUciSetOptionCommands());

        assertEquals("16", definition.getOption("Hash").getValue());
        assertNotSame(definition.getOption("Hash"), config.getOption("Hash"));
    }

    /**
     * Verifies that runtime and copied configurations do not share mutable option values.
     */
    @Test
    public void copiedRuntimeConfigsDoNotShareOptionState() {
        UciEngineConfig original = createDefinition().createRuntimeConfig(
                0,
                5,
                Map.of("Hash", "64"));
        UciEngineConfig copy = original.copy();

        copy.setOptionValue("Hash", "256");
        copy.setDepth(-10);
        copy.setMoveTimeSeconds(-4);

        assertEquals("64", original.getOption("Hash").getValue());
        assertEquals("256", copy.getOption("Hash").getValue());
        assertEquals(0, copy.getDepth());
        assertEquals(0, copy.getMoveTimeSeconds());
    }

    /**
     * Verifies that profile overrides cannot reference unknown or action-only engine options.
     */
    @Test
    public void rejectsUnknownAndButtonProfileOverrides() {
        UciEngineDefinition definition = createDefinition();

        assertThrows(
                IllegalArgumentException.class,
                () -> definition.createRuntimeConfig(0, 1, Map.of("Unknown", "value")));
        assertThrows(
                IllegalArgumentException.class,
                () -> definition.createRuntimeConfig(0, 1, Map.of("Clear Hash", "ignored")));
    }

    /**
     * Creates a representative engine definition containing configurable and action-only options.
     * @return the engine definition used by the tests
     */
    private UciEngineDefinition createDefinition() {
        LinkedHashMap<String, UciOption> options = new LinkedHashMap<>();
        options.put("Hash", new UciOption(
                UciOptionType.SPIN,
                "16",
                null,
                1,
                1024,
                List.of()));
        options.put("Ponder", new UciOption(
                UciOptionType.CHECK,
                "true",
                null,
                null,
                null,
                List.of()));
        options.put("Clear Hash", new UciOption(
                UciOptionType.BUTTON,
                null,
                null,
                null,
                null,
                List.of()));

        return new UciEngineDefinition(
                " /usr/games/test-engine ",
                " Test Engine ",
                " Test Author ",
                options);
    }
}
