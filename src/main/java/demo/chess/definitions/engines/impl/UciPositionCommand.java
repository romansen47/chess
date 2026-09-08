package demo.chess.definitions.engines.impl;

final class UciPositionCommand {

    private UciPositionCommand() {
    }

    /**
     * Builds a valid UCI position command for the standard initial position.
     * @param moves the already played moves in UCI notation
     * @return the position command
     */
    static String build(CharSequence moves) {
        String moveText = moves == null ? "" : moves.toString().trim();
        if (moveText.isEmpty()) {
            return "position startpos";
        }
        return "position startpos moves " + moveText;
    }
}
