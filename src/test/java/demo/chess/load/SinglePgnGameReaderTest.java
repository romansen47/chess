package demo.chess.load;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;

import org.junit.Test;

public class SinglePgnGameReaderTest {

    @Test
    public void preservesExactSingleGamePayload() throws Exception {
        String pgn = "\uFEFF[Event \"Single\"]\r\n"
                + "[Result \"1-0\"]\r\n"
                + "\r\n"
                + "1. e4 e5 2. Nf3 Nc6 1-0\r\n";

        SinglePgnGameReader.Result result = new SinglePgnGameReader().read(new TrackingReader(pgn, 7));

        assertEquals(1, result.gameCount());
        assertFalse(result.earlyAbort());
        assertEquals(pgn, result.content());
    }

    @Test
    public void stopsReadingWhenSecondTaggedGameStarts() throws Exception {
        String firstGame = "[Event \"First\"]\n"
                + "[Result \"1-0\"]\n"
                + "\n"
                + "1. e4 e5 1-0\n\n";
        String secondGameStart = "[Event \"Second\"]\n";
        String hugeUnusedTail = ("[Result \"0-1\"]\n\n1. d4 d5 0-1\n").repeat(100_000);
        String source = firstGame + secondGameStart + hugeUnusedTail;
        TrackingReader reader = new TrackingReader(source, 32);

        SinglePgnGameReader.Result result = new SinglePgnGameReader().read(reader);

        assertEquals(2, result.gameCount());
        assertTrue(result.earlyAbort());
        assertNull(result.content());
        assertTrue("reader must stop before consuming the complete source", reader.charsRead() < source.length());
    }

    @Test
    public void detectsSecondTaglessGameAfterResult() throws Exception {
        String source = "1. e4 e5 1-0\n\n1. d4 d5 0-1\n";

        SinglePgnGameReader.Result result = new SinglePgnGameReader().read(new TrackingReader(source, 8));

        assertEquals(2, result.gameCount());
        assertTrue(result.earlyAbort());
        assertNull(result.content());
    }

    @Test
    public void ignoresGameLikeTextInsideCommentsAndVariations() throws Exception {
        String source = "[Event \"Single\"]\n"
                + "[Result \"1-0\"]\n\n"
                + "1. e4 { comment starts\n"
                + "[Event \"Not a second game\"]\n"
                + "1. d4 d5 0-1 } e5 (1... c5 0-1) 2. Nf3 1-0\n";

        SinglePgnGameReader.Result result = new SinglePgnGameReader().read(new TrackingReader(source, 11));

        assertEquals(1, result.gameCount());
        assertFalse(result.earlyAbort());
        assertEquals(source, result.content());
    }

    @Test
    public void blankInputContainsNoGame() throws Exception {
        SinglePgnGameReader.Result result = new SinglePgnGameReader().read(new TrackingReader(" \r\n\t\n", 2));

        assertEquals(0, result.gameCount());
        assertFalse(result.earlyAbort());
        assertNull(result.content());
    }

    private static final class TrackingReader extends Reader {
        private final String source;
        private final int maxChunkSize;
        private int position;

        private TrackingReader(String source, int maxChunkSize) {
            this.source = source;
            this.maxChunkSize = maxChunkSize;
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            if (position >= source.length()) {
                return -1;
            }

            int count = Math.min(Math.min(length, maxChunkSize), source.length() - position);
            source.getChars(position, position + count, buffer, offset);
            position += count;
            return count;
        }

        @Override
        public void close() {
            // Nothing to close.
        }

        private int charsRead() {
            return position;
        }
    }
}
