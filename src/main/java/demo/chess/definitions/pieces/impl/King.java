package demo.chess.definitions.pieces.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.CastlingRights;
import demo.chess.definitions.CastlingSide;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.impl.CastlingImpl;
import demo.chess.definitions.moves.impl.ChessMove;
import demo.chess.definitions.pieces.Piece;

/** Implementation of the king piece. */
public class King extends PieceImpl {

    private static final Logger logger = LogManager.getLogger(King.class);
    private final CastlingRights castlingRights;

    public King(Color color, Field field, Board chessBoard, boolean setField) {
        this(color, field, chessBoard, setField, null);
    }

    public King(
            Color color,
            Field field,
            Board chessBoard,
            boolean setField,
            CastlingRights castlingRights) {
        super(color, field, chessBoard, setField);
        this.castlingRights = castlingRights;
    }

    @Override
    public String toString() {
        return getColor().label + "K" + getField().getName();
    }

    @Override
    public List<Move> getSimpleUnvalidatedMoves() {
        List<Move> moveList = new ArrayList<>();
        int file = getField().getFile();
        int rank = getField().getRank();
        addMove(file - 1, rank - 1, moveList);
        addMove(file - 1, rank, moveList);
        addMove(file - 1, rank + 1, moveList);
        addMove(file, rank - 1, moveList);
        addMove(file, rank + 1, moveList);
        addMove(file + 1, rank - 1, moveList);
        addMove(file + 1, rank, moveList);
        addMove(file + 1, rank + 1, moveList);
        return moveList;
    }

    @Override
    public List<Move> getPossibleUnvalidatedMoves() {
        List<Move> moveList = getSimpleUnvalidatedMoves();
        moveList.addAll(addCastlingMoves());
        return moveList;
    }

    private List<Move> addCastlingMoves() {
        List<Move> result = new ArrayList<>();
        List<Piece> movedPieces = getMoveList().stream()
                .map(Move::getPiece)
                .distinct()
                .collect(Collectors.toList());
        if (movedPieces.contains(this)) return result;

        for (CastlingSide side : CastlingSide.values()) {
            Rook rook = findCastlingRook(side);
            if (rook != null && !movedPieces.contains(rook)) {
                result.add(new CastlingImpl(this, rook, side));
            }
        }
        return result;
    }

    private Rook findCastlingRook(CastlingSide side) {
        int rank = getField().getRank();
        Integer configuredFile = castlingRights != null
                ? castlingRights.getRookFile(getColor(), side)
                : null;
        if (castlingRights != null && configuredFile == null) return null;

        if (configuredFile != null) {
            Piece piece = getChessBoard().getField(configuredFile, rank).getPiece();
            return piece instanceof Rook rook && rook.getColor() == getColor() ? rook : null;
        }

        int direction = side == CastlingSide.KING_SIDE ? 1 : -1;
        for (int file = getField().getFile() + direction; file >= 1 && file <= 8; file += direction) {
            Piece piece = getChessBoard().getField(file, rank).getPiece();
            if (piece instanceof Rook rook && rook.getColor() == getColor()) return rook;
        }
        return null;
    }

    private void addMove(int file, int rank, List<Move> moveList) {
        if (file < 1 || rank < 1 || file > 8 || rank > 8) return;
        Field targetField = getChessBoard().getField(file, rank);
        Piece targetPiece = targetField.getPiece();
        if (targetPiece == null || targetPiece.getColor() != getColor()) {
            moveList.add(new ChessMove(this, getField(), targetField));
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public PieceType getType() {
        return PieceType.KING;
    }
}
