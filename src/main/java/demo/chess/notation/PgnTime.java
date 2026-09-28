package demo.chess.notation;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conversion of PGN clock/elapsed tags; unsupported values remain ordinary comment text. */
public final class PgnTime {
    private static final Pattern TIME = Pattern.compile("(\\d+):([0-5]?\\d):([0-5]?\\d)(?:\\.(\\d{1,3}))?");

    private PgnTime() { }

    public static Long parseMillis(String text) {
        Matcher match = TIME.matcher(text.trim());
        if (!match.matches()) return null;
        try {
            long seconds = Math.addExact(Math.multiplyExact(Long.parseLong(match.group(1)), 3600),
                    Long.parseLong(match.group(2)) * 60 + Long.parseLong(match.group(3)));
            String fraction = match.group(4);
            long millis = fraction == null ? 0 : Long.parseLong((fraction + "000").substring(0, 3));
            return Math.addExact(Math.multiplyExact(seconds, 1000), millis);
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    public static String formatMillis(long millis) {
        if (millis < 0) throw new IllegalArgumentException("PGN time must not be negative");
        long seconds = millis / 1000;
        String time = String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
        return millis % 1000 == 0 ? time : time + String.format(Locale.ROOT, ".%03d", millis % 1000);
    }
}
