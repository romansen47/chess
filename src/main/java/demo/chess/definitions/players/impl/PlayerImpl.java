package demo.chess.definitions.players.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.clocks.impl.ChessClock;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.moves.impl.CastlingImpl;
import demo.chess.definitions.moves.impl.ChessMove;
import demo.chess.definitions.moves.impl.EnPassantImpl;
import demo.chess.definitions.moves.impl.PromotionImpl;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Bishop;
import demo.chess.definitions.pieces.impl.Knight;
import demo.chess.definitions.pieces.impl.Pawn;
import demo.chess.definitions.pieces.impl.Queen;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.game.impl.Simulation;

/** Abstract player implementation. */
public abstract class PlayerImpl extends PlayerSimulationBase implements Player {

    private static final long SECOND_IN_MILLIS = 1000L;

    private final List<Piece> pieces;
    private final Color color;
    private Piece king;
    private MoveList moveList;
    private ChessClock chessClock;
    private final String name;
    private int additionalTime;

    public PlayerImpl(Color color, MoveList moveList, String string) {
        this.pieces = new ArrayList<>();
        this.color = color;
        this.moveList = moveList;
        name = string;
        this.chessClock = new ChessClock();
    }

    @Override
    public void setupClock(int timeForEachPlayer, int incrementForWhite, Runnable runnable) {
        this.chessClock.setIncrementMillis(incrementForWhite * SECOND_IN_MILLIS);
        this.chessClock.setTargetTimeMillis(timeForEachPlayer * SECOND_IN_MILLIS);
        this.chessClock.setTimeUpAction(runnable);
    }

    @Override
    public List<Move> getSimpleMoves() {
        List<Move> possibleValidMoves = new ArrayList<>();
        for (Piece piece : getPieces()) possibleValidMoves.addAll(piece.getSimpleUnvalidatedMoves());
        return possibleValidMoves;
    }

    @Override
    public List<Move> getValidMoves(Game chessGame) throws NoMoveFoundException, IOException {
        List<Move> possibleUnvalidatedMoves = new ArrayList<>();
        List<Move> possibleValidMoves = new ArrayList<>();
        for (Piece piece : getPieces()) possibleUnvalidatedMoves.addAll(piece.getPossibleUnvalidatedMoves());
        for (Move move : possibleUnvalidatedMoves) if (simulate(chessGame, move)) possibleValidMoves.add(move);
        return possibleValidMoves;
    }

    @Override
    public void resignOrStaleMate(Game chessGame) {
        State terminalState = TerminalPositionEvaluator.determineStateWhenNoLegalMoves(chessGame);
        if (terminalState != null) chessGame.setState(terminalState);
        if (chessGame.getWhitePlayer().getChessClock().isStarted()) chessGame.getWhitePlayer().getChessClock().stop();
        if (chessGame.getBlackPlayer().getChessClock().isStarted()) chessGame.getBlackPlayer().getChessClock().stop();
    }

    @Override
    public Move replaceByValidMove(Game chessGame, Move move) throws NoMoveFoundException, IOException {
        for (Move realMove : getValidMoves(chessGame)) if (realMove.equals(move)) return realMove;
        return null;
    }

    private boolean validateCastling(Game chessGame, Move move) {
        if (!(move instanceof Castling castling)) return false;
        Rook rook = castling.getRook();
        if (rook == null || king == null || king.getField() == null || rook.getField() == null) return false;
        if (rook.getColor() != getColor()) return false;

        Integer expectedRookFile = chessGame.getCastlingRights().getRookFile(getColor(), castling.getSide());
        if (expectedRookFile == null || expectedRookFile != rook.getField().getFile()) return false;

        boolean kingMoved = chessGame.getMoveList().stream().anyMatch(previous -> previous.getPiece() == king);
        boolean rookMoved = chessGame.getMoveList().stream().anyMatch(previous -> previous.getPiece() == rook);
        if (kingMoved || rookMoved) return false;

        Field kingSource = king.getField();
        Field rookSource = rook.getField();
        Field kingTarget = castling.getKingTarget();
        Field rookTarget = castling.getRookTarget();
        if (kingSource.getRank() != rookSource.getRank()
                || kingSource.getRank() != kingTarget.getRank()
                || kingSource.getRank() != rookTarget.getRank()) return false;

        if (!pathClear(chessGame, kingSource, kingTarget, kingSource, rookSource)
                || !pathClear(chessGame, rookSource, rookTarget, kingSource, rookSource)) return false;

        Player opponent = getColor() == Color.WHITE ? chessGame.getBlackPlayer() : chessGame.getWhitePlayer();
        int direction = Integer.compare(kingTarget.getFile(), kingSource.getFile());
        if (direction == 0) return !isAttacked(kingSource, opponent);
        for (int file = kingSource.getFile(); ; file += direction) {
            if (isAttacked(chessGame.getChessBoard().getField(file, kingSource.getRank()), opponent)) return false;
            if (file == kingTarget.getFile()) break;
        }
        return true;
    }

    private boolean pathClear(Game game, Field from, Field to, Field kingSource, Field rookSource) {
        int min = Math.min(from.getFile(), to.getFile());
        int max = Math.max(from.getFile(), to.getFile());
        int rank = from.getRank();
        for (int file = min; file <= max; file++) {
            Field field = game.getChessBoard().getField(file, rank);
            if (field.equals(kingSource) || field.equals(rookSource)) continue;
            if (field.getPiece() != null) return false;
        }
        return true;
    }

    private boolean isAttacked(Field target, Player opponent) {
        for (Piece piece : opponent.getPieces()) {
            if (piece == null || piece.getField() == null) continue;
            if (piece instanceof Pawn pawn) {
                int direction = pawn.getColor() == Color.WHITE ? 1 : -1;
                if (target.getRank() == pawn.getField().getRank() + direction
                        && Math.abs(target.getFile() - pawn.getField().getFile()) == 1) return true;
                continue;
            }
            for (Move attack : piece.getSimpleUnvalidatedMoves()) {
                if (attack instanceof Castling || attack.getTarget() == null) continue;
                if (attack.getTarget().equals(target)) return true;
            }
        }
        return false;
    }

    @Override
    protected boolean simulate(Game chessGame, Move move) throws NoMoveFoundException, IOException {
        if (move instanceof Castling && !validateCastling(chessGame, move)) return false;
        Game simulation = Simulation.createSimulation(chessGame.getStartingPosition());
        for (Move previous : getMoveList()) {
            Move newMove = getMoveInSimulation(simulation, previous);
            if (newMove == null) return false;
            simulation.apply(newMove);
        }
        Player originalPlayer = simulation.getPlayer();
        Move simulatedMove = getMoveInSimulation(simulation, move);
        if (simulatedMove == null) return false;
        simulation.apply(simulatedMove);
        Player otherPlayer = simulation.getPlayer();
        Field kingField = originalPlayer.getKing().getField();
        for (Piece opponentPiece : otherPlayer.getPieces()) {
            if (opponentPiece == null || opponentPiece.getField() == null) continue;
            if (opponentPiece instanceof Pawn pawn) {
                int direction = pawn.getColor() == Color.WHITE ? 1 : -1;
                if (kingField.getRank() == pawn.getField().getRank() + direction
                        && Math.abs(kingField.getFile() - pawn.getField().getFile()) == 1) return false;
                continue;
            }
            boolean attacked = opponentPiece.getSimpleUnvalidatedMoves().stream()
                    .filter(candidate -> !(candidate instanceof Castling))
                    .map(Move::getTarget)
                    .anyMatch(kingField::equals);
            if (attacked) return false;
        }
        return true;
    }

    @Override
    public Move getMoveInSimulation(Game simulation, Move move) {
        Board chessBoard = simulation.getChessBoard();
        Field source = chessBoard.getField(move.getSource().getFile(), move.getSource().getRank());
        Field target = chessBoard.getField(move.getTarget().getFile(), move.getTarget().getRank());
        Piece piece = source.getPiece();
        if (piece == null) return null;

        if (move instanceof Promotion promotion) {
            Piece promotedPiece = promotion.getPromotedPiece();
            Piece simulatedPromotedPiece = switch (promotedPiece.getType()) {
                case QUEEN -> new Queen(promotedPiece.getColor(), target, chessBoard, false);
                case ROOK -> new Rook(promotedPiece.getColor(), target, chessBoard, false);
                case KNIGHT -> new Knight(promotedPiece.getColor(), target, chessBoard, false);
                case BISHOP -> new Bishop(promotedPiece.getColor(), target, chessBoard, false);
                default -> null;
            };
            return new PromotionImpl(piece, source, target, simulatedPromotedPiece);
        }
        if (move instanceof EnPassant ep) {
            Field slayedField = chessBoard.getField(
                    ep.getSlayedPiece().getField().getFile(),
                    ep.getSlayedPiece().getField().getRank());
            return new EnPassantImpl(piece, source, target, (Pawn) slayedField.getPiece());
        }
        if (move instanceof Castling castling) {
            Piece targetPiece = target.getPiece();
            if (!(targetPiece instanceof Rook rook)) return null;
            return new CastlingImpl(piece, rook, castling.getSide());
        }
        return new ChessMove(piece, source, target);
    }

    @Override
    public void reset() {
        pieces.clear();
    }

    @Override
    public Piece getKing() {
        return king;
    }

    @Override
    public void setKing(Piece king) {
        this.king = king;
    }

    @Override
    public MoveList getMoveList() {
        return moveList;
    }

    public void setMoveList(MoveList moveList) {
        this.moveList = moveList;
    }

    @Override
    public ChessClock getChessClock() {
        return chessClock;
    }

    @Override
    public void setChessClock(ChessClock chessClock) {
        this.chessClock = chessClock;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Piece> getPieces() {
        return pieces;
    }

    @Override
    public int getAdditionalTime() {
        return additionalTime;
    }

    @Override
    public void setAdditionalTime(int additionalTime) {
        this.additionalTime = additionalTime;
    }

    @Override
    public String toString() {
        return " PLAYER: created for " + name;
    }
}
