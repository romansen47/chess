package demo.chess.definitions.engines.uci;

import java.util.Optional;

/** Parses the subset of UCI {@code info} output used by CAT evaluation views. */
public final class UciInfoParser {

    private UciInfoParser() {
    }

    /**
     * Parses one scored principal-variation line.
     *
     * <p>Diagnostic {@code info string} output, {@code currmove} progress lines,
     * malformed values and unscored PV lines are deliberately ignored. This
     * keeps engine-specific diagnostic text from influencing search depth.</p>
     *
     * @param line raw engine output
     * @return parsed PV snapshot, or empty when the line is not usable
     */
    public static Optional<UciInfoLine> parsePrincipalVariation(String line) {
        if (line == null) return Optional.empty();
        String trimmed = line.trim();
        if (!trimmed.startsWith("info ")) return Optional.empty();

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length < 2 || !"info".equals(tokens[0]) || "string".equals(tokens[1])) {
            return Optional.empty();
        }

        Integer depth = null;
        int multiPv = 1;
        Integer centipawnScore = null;
        Integer mateScore = null;
        String pv = null;

        for (int index = 1; index < tokens.length; index++) {
            String token = tokens[index];
            try {
                switch (token) {
                    case "depth" -> {
                        if (index + 1 >= tokens.length) return Optional.empty();
                        depth = Integer.parseInt(tokens[++index]);
                    }
                    case "multipv" -> {
                        if (index + 1 >= tokens.length) return Optional.empty();
                        multiPv = Integer.parseInt(tokens[++index]);
                    }
                    case "score" -> {
                        if (index + 2 >= tokens.length) return Optional.empty();
                        String scoreType = tokens[++index];
                        int scoreValue = Integer.parseInt(tokens[++index]);
                        if ("cp".equals(scoreType)) {
                            centipawnScore = scoreValue;
                            mateScore = null;
                        } else if ("mate".equals(scoreType)) {
                            mateScore = scoreValue;
                            centipawnScore = null;
                        }
                    }
                    case "pv" -> {
                        if (index + 1 >= tokens.length) return Optional.empty();
                        StringBuilder moves = new StringBuilder();
                        for (int moveIndex = index + 1; moveIndex < tokens.length; moveIndex++) {
                            if (!moves.isEmpty()) moves.append(' ');
                            moves.append(tokens[moveIndex]);
                        }
                        pv = moves.toString();
                        index = tokens.length;
                    }
                    default -> {
                        // Other UCI info fields are intentionally ignored.
                    }
                }
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        if (depth == null || depth < 0 || multiPv < 1 || pv == null || pv.isBlank()) {
            return Optional.empty();
        }
        if (centipawnScore == null && mateScore == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(new UciInfoLine(
                    depth,
                    multiPv,
                    centipawnScore,
                    mateScore,
                    pv));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
