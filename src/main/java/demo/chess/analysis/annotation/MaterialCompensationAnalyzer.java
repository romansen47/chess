package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import demo.chess.definitions.Color;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.Player;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/**
 * Diagnostic-only bounded proof search for short-term material compensation.
 *
 * <p>This class deliberately does not participate in move classification.
 * It only refines the explanation of an already detected passive material
 * offer. The sacrificing side may choose captures, checks or promotions;
 * the defender may choose any legal reply. A result is returned only when
 * compensation can be proved within the small search horizon and node budget.</p>
 */
final class MaterialCompensationAnalyzer {

    private static final int MAX_RECOVERY_PLIES = 4;
    private static final int MAX_SEARCH_NODES = 512;

    Evidence find(
            Game rootPosition,
            String playedMoveUci,
            MaterialSacrificeEvidence sacrifice,
            boolean whiteMover) {
        if (rootPosition == null
                || playedMoveUci == null
                || playedMoveUci.isBlank()
                || sacrifice == null
                || sacrifice.getType()
                        == MaterialSacrificeType.ACTIVE_INVESTMENT
                || sacrifice.getAcceptanceMoveUci() == null) {
            return null;
        }

        try {
            Game afterAcceptance =
                    Simulation.forkSimulationFrom(
                            rootPosition.getMoveList());
            double rootBalance =
                    MaterialInvestmentDetector.materialBalanceForMover(
                            afterAcceptance,
                            whiteMover);

            apply(afterAcceptance, playedMoveUci);
            apply(afterAcceptance, sacrifice.getAcceptanceMoveUci());

            SearchBudget budget =
                    new SearchBudget(MAX_SEARCH_NODES);
            SearchResult result = search(
                    afterAcceptance,
                    whiteMover,
                    rootBalance,
                    0,
                    budget);

            return result.compensated()
                    ? new Evidence(result.plies())
                    : null;
        } catch (Exception ignored) {
            /*
             * This is explanation-only diagnostics. A replay/search problem
             * must never change or break the underlying annotation.
             */
            return null;
        }
    }

    private SearchResult search(
            Game position,
            boolean whiteMover,
            double rootBalance,
            int depth,
            SearchBudget budget)
            throws Exception {
        if (!budget.consume()) {
            return SearchResult.notCompensated();
        }

        boolean moverTurn = isMoverTurn(position, whiteMover);
        double remainingLoss = Math.max(
                0.0,
                rootBalance
                        - MaterialInvestmentDetector
                                .materialBalanceForMover(
                                        position,
                                        whiteMover));

        /*
         * Reaching a mover turn below the normal sacrifice threshold means
         * the opponent has already had the opportunity to answer the
         * compensating move and could not restore a qualifying material loss.
         */
        if (moverTurn
                && remainingLoss
                        < MoveAnnotationPolicy
                                .EXTRAORDINARY_MATERIAL_INVESTMENT) {
            return SearchResult.compensated(depth);
        }

        List<Move> legalMoves =
                position.getPlayer().getValidMoves(position);
        if (legalMoves.isEmpty()) {
            return remainingLoss
                    < MoveAnnotationPolicy
                            .EXTRAORDINARY_MATERIAL_INVESTMENT
                    ? SearchResult.compensated(depth)
                    : SearchResult.notCompensated();
        }

        if (depth >= MAX_RECOVERY_PLIES) {
            return SearchResult.notCompensated();
        }

        if (moverTurn) {
            List<MoveCandidate> forcing = new ArrayList<>();
            for (Move move : legalMoves) {
                MoveCandidate candidate =
                        createCandidate(position, move);
                if (candidate.forcing()) {
                    forcing.add(candidate);
                }
            }

            forcing.sort(
                    Comparator.comparingInt(
                            MoveCandidate::priority)
                            .reversed());

            SearchResult best =
                    SearchResult.notCompensated();
            for (MoveCandidate candidate : forcing) {
                SearchResult child = search(
                        candidate.position(),
                        whiteMover,
                        rootBalance,
                        depth + 1,
                        budget);
                if (child.compensated()
                        && (!best.compensated()
                                || child.plies() < best.plies())) {
                    best = child;
                }
            }
            return best;
        }

        int requiredPlies = depth;
        for (Move move : legalMoves) {
            Game child = replay(position, move);
            SearchResult branch = search(
                    child,
                    whiteMover,
                    rootBalance,
                    depth + 1,
                    budget);
            if (!branch.compensated()) {
                return SearchResult.notCompensated();
            }
            requiredPlies =
                    Math.max(requiredPlies, branch.plies());
        }
        return SearchResult.compensated(requiredPlies);
    }

    private MoveCandidate createCandidate(
            Game position,
            Move move)
            throws Exception {
        Piece captured = capturedPiece(move);
        boolean capture = captured != null;
        boolean promotion = move instanceof Promotion;
        int captureValue = capture
                ? (int) Math.round(
                        MaterialInvestmentDetector.pieceValue(
                                captured.getType()) * 100.0)
                : 0;

        Game child = replay(position, move);
        boolean check = sideToMoveIsInCheck(child);
        int priority =
                captureValue
                        + (promotion ? 50 : 0)
                        + (check ? 10 : 0);
        return new MoveCandidate(
                child,
                capture || promotion || check,
                priority);
    }

    private Game replay(
            Game position,
            Move move)
            throws Exception {
        Game child =
                Simulation.forkSimulationFrom(
                        position.getMoveList());
        try {
            Move replayed =
                    LegalMoveResolver.resolveUci(
                            child,
                            move.toString());
            child.apply(replayed);
            return child;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not replay diagnostic move "
                            + move,
                    exception);
        }
    }

    private void apply(
            Game position,
            String moveUci)
            throws Exception {
        Move move =
                LegalMoveResolver.resolveUci(
                        position,
                        moveUci);
        position.apply(move);
    }

    private boolean isMoverTurn(
            Game position,
            boolean whiteMover) {
        if (position.getPlayer() == null
                || position.getPlayer().getColor() == null) {
            return false;
        }
        Color mover = whiteMover
                ? Color.WHITE
                : Color.BLACK;
        return position.getPlayer().getColor() == mover;
    }

    private boolean sideToMoveIsInCheck(Game position) {
        Player sideToMove = position.getPlayer();
        if (sideToMove == null
                || sideToMove.getKing() == null
                || sideToMove.getKing().getField() == null) {
            return false;
        }

        Player attacker =
                sideToMove.getColor() == Color.WHITE
                        ? position.getBlackPlayer()
                        : position.getWhitePlayer();
        if (attacker == null) {
            return false;
        }

        return attacker.getSimpleMoves().stream()
                .map(Move::getTarget)
                .filter(target -> target != null)
                .anyMatch(
                        sideToMove.getKing()
                                .getField()::equals);
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

    private record MoveCandidate(
            Game position,
            boolean forcing,
            int priority) {
    }

    private record SearchResult(
            boolean compensated,
            int plies) {

        static SearchResult compensated(int plies) {
            return new SearchResult(true, plies);
        }

        static SearchResult notCompensated() {
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
