package demo.chess.definitions.engines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class UciEngineInspectorTest {

    /**
     * Verifies parsing of a numeric UCI option including its default and bounds.
     */
    @Test
    public void parsesSpinOptionMetadata() {
        Map.Entry<String, UciOption> parsed = UciEngineInspector.parseOptionLine(
                "option name Hash type spin default 64 min 1 max 2048");

        assertEquals("Hash", parsed.getKey());
        assertEquals(UciOptionType.SPIN, parsed.getValue().getType());
        assertEquals("64", parsed.getValue().getDefaultValue());
        assertEquals(Integer.valueOf(1), parsed.getValue().getMin());
        assertEquals(Integer.valueOf(2048), parsed.getValue().getMax());
    }

    /**
     * Verifies parsing of combo values, including values containing spaces.
     */
    @Test
    public void parsesComboOptionWithMultiWordVariants() {
        Map.Entry<String, UciOption> parsed = UciEngineInspector.parseOptionLine(
                "option name Style type combo default Normal var Solid var Normal var Very Aggressive");

        assertEquals("Style", parsed.getKey());
        assertEquals(UciOptionType.COMBO, parsed.getValue().getType());
        assertEquals("Normal", parsed.getValue().getDefaultValue());
        assertEquals(List.of("Solid", "Normal", "Very Aggressive"), parsed.getValue().getVars());
    }

    /**
     * Verifies the special empty-string and button semantics defined by the UCI protocol.
     */
    @Test
    public void parsesStringAndButtonOptions() {
        Map.Entry<String, UciOption> stringOption = UciEngineInspector.parseOptionLine(
                "option name SyzygyPath type string default <empty>");
        Map.Entry<String, UciOption> buttonOption = UciEngineInspector.parseOptionLine(
                "option name Clear Hash type button");

        assertEquals("", stringOption.getValue().getDefaultValue());
        assertEquals("", stringOption.getValue().getValue());
        assertEquals(UciOptionType.BUTTON, buttonOption.getValue().getType());
        assertNull(buttonOption.getValue().getDefaultValue());
        assertNull(buttonOption.getValue().getValue());
    }

    /**
     * Verifies that malformed UCI option lines fail fast instead of creating ambiguous definitions.
     */
    @Test
    public void rejectsMalformedOptionLines() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UciEngineInspector.parseOptionLine("id name Not An Option"));
        assertThrows(
                IllegalArgumentException.class,
                () -> UciEngineInspector.parseOptionLine("option name Hash default 16"));
        assertThrows(
                IllegalArgumentException.class,
                () -> UciEngineInspector.parseOptionLine("option name  type spin default 16 min 1 max 128"));
        assertThrows(
                IllegalArgumentException.class,
                () -> UciEngineInspector.parseOptionLine("option name Hash type spin default 16 min invalid max 128"));
    }
}
