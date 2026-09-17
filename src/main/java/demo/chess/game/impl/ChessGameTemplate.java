package demo.chess.game.impl;

import java.io.IOException;
import java.util.List;

import demo.chess.definitions.CastlingRights;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Bishop;
import demo.chess.definitions.pieces.impl.King;
import demo.chess.definitions.pieces.impl.Knight;
import demo.chess.definitions.pieces.impl.Pawn;
import demo.chess.definitions.pieces.impl.Queen;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.players.WhitePlayer;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;

/** Abstract core game implementation shared by live games and simulations. */
public abstract class ChessGameTemplate implements Game {

    private Player player;
    private final WhitePlayer whitePlayer;
    private final BlackPlayer blackPlayer;
    private final Board chessBoard;
    private final MoveList moveList;
    private final ChessStartingPosition startingPosition;
    private final CastlingRights castlingRights;
    private volatile State state = null;

    public ChessGameTemplate(
            Board chessBoard,
            WhitePlayer whitePlayer,
            BlackPlayer blackPlayer,
            MoveList moveList) {
        this(chessBoard, whitePlayer, blackPlayer, moveList, ChessStartingPosition.STANDARD);
    }

    public ChessGameTemplate(
            Board chessBoard,
            WhitePlayer whitePlayer,
            BlackPlayer blackPlayer,
            MoveList moveList,
            ChessStartingPosition startingPosition) {
        this.player = whitePlayer;
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.chessBoard = chessBoard;
        this.moveList = moveList;
        this.startingPosition = startingPosition != null
                ? startingPosition
                : ChessStartingPosition.STANDARD;
        this.castlingRights = new CastlingRights(this.startingPosition);
        this.moveList.setStartingPosition(this.startingPosition);
        createPieces();
    }

    @Override
    public void createPieces() {
        List<Piece> whitePieces = whitePlayer.getPieces();
        List<Piece> blackPieces = blackPlayer.getPieces();

        for (int file = 1; file <= 8; file++) {
            whitePieces.add(new Pawn(Color.WHITE, chessBoard.getField(file, 2), chessBoard, true));
            blackPieces.add(new Pawn(Color.BLACK, chessBoard.getField(file, 7), chessBoard, true));
        }

        for (int file = 1; file <= 8; file++) {
            addBackRankPiece(whitePieces, Color.WHITE, file, 1);
            addBackRankPiece(blackPieces, Color.BLACK, file, 8);
        }

        whitePieces.forEach(piece -> piece.setMoveList(moveList));
        blackPieces.forEach(piece -> piece.setMoveList(moveList));
    }

    private void addBackRankPiece(List<Piece> pieces, Color color, int file, int rank) {
        PieceType type = startingPosition.getPieceTypeAtFile(file);
        Field field = chessBoard.getField(file, rank);
        Piece piece = switch (type) {
            case ROOK -> new Rook(color, field, chessBoard, true);
            case KNIGHT -> new Knight(color, field, chessBoard, true);
            case BISHOP -> new Bishop(color, field, chessBoard, true);
            case QUEEN -> new Queen(color, field, chessBoard, true);
            case KING -> new King(color, field, chessBoard, true, castlingRights);
            case PAWN -> throw new IllegalStateException("Pawn cannot occur on a Chess960 back rank");
        };
        pieces.add(piece);
        if (type == PieceType.KING) {
            if (color == Color.WHITE) whitePlayer.setKing(piece);
            else blackPlayer.setKing(piece);
        }
    }

    @Override
    public Board getChessBoard() {
        return chessBoard;
    }

    @Override
    public Player getWhitePlayer() {
        return whitePlayer;
    }

    @Override
    public Player getBlackPlayer() {
        return blackPlayer;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public MoveList getMoveList() {
        return moveList;
    }

    @Override
    public ChessStartingPosition getStartingPosition() {
        return startingPosition;
    }

    @Override
    public CastlingRights getCastlingRights() {
        return castlingRights;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        if (this.getWhitePlayer().getChessClock().isStarted()) {
            this.getWhitePlayer().getChessClock().stop();
        }
        if (this.getBlackPlayer().getChessClock().isStarted()) {
            this.getBlackPlayer().getChessClock().stop();
        }
        this.state = state;
    }

    @Override
    public void switchPlayer() {
        if (getPlayer() == getWhitePlayer()) setPlayer(getBlackPlayer());
        else setPlayer(getWhitePlayer());
    }

    @Override
    public void apply(Move move) throws NoMoveFoundException, IOException {
        updateCastlingRights(move);
        boolean applied = false;
        if (move instanceof EnPassant) {
            applyEnPassant(move);
            applied = true;
        }
        if (move instanceof Promotion promotion) {
            applyPromotion(promotion);
            applied = true;
        }
        if (move instanceof Castling castling) {
            applyCastling(castling);
            applied = true;
        }
        if (!applied) applyRegularMove(move);
    }

    private void updateCastlingRights(Move move) {
        if (move == null || move.getPiece() == null || move.getSource() == null) return;

        Piece movingPiece = move.getPiece();
        if (movingPiece.getType() == PieceType.KING) {
            castlingRights.disableAll(movingPiece.getColor());
        } else if (movingPiece.getType() == PieceType.ROOK) {
            castlingRights.disableRookAt(movingPiece.getColor(), move.getSource().getFile());
        }

        if (!(move instanceof Castling) && move.getTarget() != null) {
            Piece captured = move.getTarget().getPiece();
            if (captured != null && captured.getType() == PieceType.ROOK) {
                castlingRights.disableRookAt(captured.getColor(), move.getTarget().getFile());
            }
        }
    }

    private void applyCastling(Castling move) {
        King king = (King) move.getPiece();
        Rook rook = move.getRook();
        Field kingSource = king.getField();
        Field rookSource = rook.getField();
        Field kingTarget = move.getKingTarget();
        Field rookTarget = move.getRookTarget();

        kingSource.setPiece(null);
        if (!rookSource.equals(kingSource)) rookSource.setPiece(null);

        king.setField(kingTarget);
        kingTarget.setPiece(king);
        rook.setField(rookTarget);
        rookTarget.setPiece(rook);

        getMoveList().add(move);
        switchPlayer();
    }

    private void applyPromotion(Promotion move) {
        Field source = move.getSource();
        Field target = move.getTarget();
        Piece sourcePiece = move.getPiece();
        Piece promotedPiece = move.getPromotedPiece();
        Piece targetPiece = move.getTarget().getPiece();

        sourcePiece.setField(null);
        promotedPiece.setField(target);
        promotedPiece.setMoveList(getMoveList());
        target.setPiece(promotedPiece);
        getPlayer().getPieces().add(promotedPiece);
        getPlayer().getPieces().remove(sourcePiece);
        source.setPiece(null);
        getMoveList().add(move);
        switchPlayer();

        if (targetPiece != null) {
            targetPiece.setField(null);
            getPlayer().getPieces().remove(targetPiece);
        }
    }

    private void applyRegularMove(Move move) {
        Field source = move.getSource();
        Field target = move.getTarget();
        Piece sourcePiece = move.getPiece();
        Piece targetPiece = move.getTarget().getPiece();
        sourcePiece.setField(target);
        target.setPiece(sourcePiece);
        source.setPiece(null);
        getMoveList().add(move);
        switchPlayer();

        if (targetPiece != null) {
            targetPiece.setField(null);
            getPlayer().getPieces().remove(targetPiece);
        }
    }

    private void applyEnPassant(Move move) {
        EnPassant enPassant = (EnPassant) move;
        Field source = enPassant.getSource();
        Field target = enPassant.getTarget();
        Pawn sourcePawn = (Pawn) enPassant.getPiece();
        sourcePawn.setField(target);
        target.setPiece(sourcePawn);
        source.setPiece(null);

        Pawn slayedPawn = enPassant.getSlayedPiece();
        Field fieldOfSlayedPawn = slayedPawn.getField();
        fieldOfSlayedPawn.setPiece(null);
        getMoveList().add(move);
        switchPlayer();
        getPlayer().getPieces().remove(slayedPawn);
    }
}
