package demo.chess.analysis.annotation;

/** Human-readable reason why a move is mechanically natural. */
enum RoutineMoveReason {
    NOT_ROUTINE,
    ONLY_LEGAL_MOVE,
    SAFE_RETREAT_AFTER_NEW_ATTACK,
    EVEN_IMMEDIATE_RECAPTURE;

    boolean isRoutine() {
        return this != NOT_ROUTINE;
    }
}
