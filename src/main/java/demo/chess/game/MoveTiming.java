package demo.chess.game;

/**
 * Clock information captured when one live-game half-move is completed.
 *
 * @param clockMillis remaining time of the mover after increment/additional time
 * @param elapsedMoveMillis active clock time consumed for this move
 */
public record MoveTiming(long clockMillis, long elapsedMoveMillis) {

    public MoveTiming {
        if (clockMillis < 0 || elapsedMoveMillis < 0) {
            throw new IllegalArgumentException("Move timing values must not be negative");
        }
    }
}
