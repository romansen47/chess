package demo.chess.analysis.annotation;

import java.util.List;

import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/**
 * Proves that a restored material balance survives the immediate causal
 * capture sequence.
 *
 * <p>The recovery move itself is selected by
 * {@link MaterialCompensationAnalyzer}. This class starts once the original
 * loss has been recovered and limits further analysis to captures. If an
 * opponent capture recreates a qualifying loss, any mover piece may directly
 * recapture the piece that caused it. Unrelated quiet play is outside this
 * material-exchange responsibility.</p>
 */
final class MaterialExchangeClosureAnalyzer {

    private static final int MAX_SEARCH_NODES = 256;

    Evidence find(
            Game position,
            boolean whiteMover,
            double rootBalance,
            int maxPlies) {
        if (position == null || maxPlies < 0) {
            return null;
        }

        try {
            SearchResult result = proveClosure(
                    position,
                    whiteMover,
                    rootBalance,
                    0,
                    maxPlies,
                    new SearchBudget(MAX_SEARCH_NODES));
            return result.stable()
                    ? new Evidence(result.plies())
                    : null;
        } catch (Exception ignored) {
            // An incomplete exchange is unknown, never proof of recovery.
            return null;
        }
    }

    private SearchResult proveClosure(
            Game position,
            boolean whiteMover,
            double rootBalance,
            int depth,
            int maxPlies,
            SearchBudget budget)
            throws Exception {
        if (!budget.consume()) {
            return SearchResult.notStable();
        }

        int requiredPlies = depth;
        List<Move> replies = position.getPlayer().getValidMoves(position);
        for (Move reply : replies) {
            if (capturedPiece(reply) == null) {
                continue;
            }

            Game afterCapture = replay(position, reply);
            int replyDepth = depth + 1;
            double remainingLoss = remainingLoss(
                    afterCapture,
                    whiteMover,
                    rootBalance);
            if (remainingLoss
                    < MoveAnnotationPolicy
                            .EXTRAORDINARY_MATERIAL_INVESTMENT) {
                requiredPlies = Math.max(requiredPlies, replyDepth);
                continue;
            }

            if (replyDepth >= maxPlies) {
                return SearchResult.notStable();
            }

            Piece capturingPiece = pieceOnTarget(afterCapture, reply);
            SearchResult recapture = proveDirectRecapture(
                    afterCapture,
                    capturingPiece,
                    whiteMover,
                    rootBalance,
                    replyDepth,
                    maxPlies,
                    budget);
            if (!recapture.stable()) {
                return SearchResult.notStable();
            }
            requiredPlies = Math.max(
                    requiredPlies,
                    recapture.plies());
        }

        return SearchResult.stable(requiredPlies);
    }

    private SearchResult proveDirectRecapture(
            Game position,
            Piece capturingPiece,
            boolean whiteMover,
            double rootBalance,
            int depth,
            int maxPlies,
            SearchBudget budget)
            throws Exception {
        if (capturingPiece == null || !budget.consume()) {
            return SearchResult.notStable();
        }

        SearchResult best = SearchResult.notStable();
        for (Move move : position.getPlayer().getValidMoves(position)) {
            if (capturedPiece(move) != capturingPiece) {
                continue;
            }

            int recaptureDepth = depth + 1;
            if (recaptureDepth > maxPlies) {
                continue;
            }

            Game afterRecapture = replay(position, move);
            if (remainingLoss(
                    afterRecapture,
                    whiteMover,
                    rootBalance)
                    >= MoveAnnotationPolicy
                            .EXTRAORDINARY_MATERIAL_INVESTMENT) {
                continue;
            }

            SearchResult closure = proveClosure(
                    afterRecapture,
                    whiteMover,
                    rootBalance,
                    recaptureDepth,
                    maxPlies,
                    budget);
            if (closure.stable()
                    && (!best.stable()
                            || closure.plies() < best.plies())) {
                best = closure;
            }
        }
        return best;
    }

    private double remainingLoss(
            Game position,
            boolean whiteMover,
            double rootBalance) {
        return Math.max(
                0.0,
                rootBalance
                        - MaterialInvestmentDetector
                                .materialBalanceForMover(
                                        position,
                                        whiteMover));
    }

    private Game replay(Game position, Move move) throws Exception {
        Game child = Simulation.forkSimulationFrom(position.getMoveList());
        Move replayed = LegalMoveResolver.resolveUci(
                child,
                move.toString());
        child.apply(replayed);
        return child;
    }

    private Piece pieceOnTarget(Game position, Move move) {
        if (move == null || move.getTarget() == null) {
            return null;
        }
        return position.getChessBoard()
                .getField(
                        move.getTarget().getFile(),
                        move.getTarget().getRank())
                .getPiece();
    }

    private Piece capturedPiece(Move move) {
        if (move instanceof EnPassant enPassant) {
            return enPassant.getSlayedPiece();
        }
        return move != null && move.getTarget() != null
                ? move.getTarget().getPiece()
                : null;
    }

    record Evidence(int plies) {
    }

    private record SearchResult(boolean stable, int plies) {

        static SearchResult stable(int plies) {
            return new SearchResult(true, plies);
        }

        static SearchResult notStable() {
            return new SearchResult(false, Integer.MAX_VALUE);
        }
    }

    private static final class SearchBudget {

        private int remaining;

        SearchBudget(int remaining) {
            this.remaining = remaining;
        }

        boolean consume() {
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }
}
