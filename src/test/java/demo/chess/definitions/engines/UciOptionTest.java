package demo.chess.definitions.engines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class UciOptionTest {

    /**
     * Verifies that boolean UCI options are normalized and reject non-boolean values.
     */
    @Test
    public void checkOptionsNormalizeValuesAndRejectInvalidInput() {
        UciOption option = new UciOption(
                UciOptionType.CHECK,
                " TRUE ",
                null,
                null,
                null,
                List.of());

        assertEquals("true", option.getDefaultValue());
        assertEquals("true", option.getValue());

        option.setValue(" FALSE ");

        assertEquals("false", option.getValue());
        assertEquals("setoption name Ponder value false", option.toSetOptionCommand("Ponder"));
        assertThrows(IllegalArgumentException.class, () -> option.setValue("yes"));
    }

    /**
     * Verifies that numeric UCI options enforce their declared range and integer format.
     */
    @Test
    public void spinOptionsEnforceBoundsAndIntegerValues() {
        UciOption option = new UciOption(
                UciOptionType.SPIN,
                "16",
                null,
                1,
                128,
                List.of());

        option.setValue(" 128 ");

        assertEquals("128", option.getValue());
        assertEquals("setoption name Hash value 128", option.toSetOptionCommand("Hash"));
        assertThrows(IllegalArgumentException.class, () -> option.setValue("0"));
        assertThrows(IllegalArgumentException.class, () -> option.setValue("129"));
        assertThrows(IllegalArgumentException.class, () -> option.setValue("many"));
    }

    /**
     * Verifies that combo options accept only values advertised by the engine.
     */
    @Test
    public void comboOptionsAcceptOnlyAdvertisedValues() {
        UciOption option = new UciOption(
                UciOptionType.COMBO,
                "Normal",
                null,
                null,
                null,
                List.of("Solid", "Normal", "Very Aggressive"));

        option.setValue("Very Aggressive");

        assertEquals("Very Aggressive", option.getValue());
        assertThrows(IllegalArgumentException.class, () -> option.setValue("Unknown"));
        assertThrows(UnsupportedOperationException.class, () -> option.getVars().add("Injected"));
    }

    /**
     * Verifies the UCI representation of empty string options and non-configurable button options.
     */
    @Test
    public void stringAndButtonOptionsUseCorrectSetOptionSemantics() {
        UciOption stringOption = new UciOption(
                UciOptionType.STRING,
                "<empty>",
                null,
                null,
                null,
                List.of());
        UciOption buttonOption = new UciOption(
                UciOptionType.BUTTON,
                null,
                null,
                null,
                null,
                List.of());

        assertEquals("", stringOption.getDefaultValue());
        assertEquals("", stringOption.getValue());
        assertEquals("setoption name SyzygyPath value", stringOption.toSetOptionCommand("SyzygyPath"));

        assertFalse(buttonOption.isConfigurable());
        assertEquals("", buttonOption.toSetOptionCommand("Clear Hash"));
        assertTrue(stringOption.isConfigurable());
    }
}
