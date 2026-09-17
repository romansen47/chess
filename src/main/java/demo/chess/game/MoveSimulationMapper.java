package demo.chess.game;

import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
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

/**
 * Rebinds a domain move to the corresponding pieces and fields of a simulation.
 *
 * <p>A move object belongs to the board on which it was created. Replaying it on
 * a fork therefore requires rebuilding the move with the fork's field and piece
 * instances. Keeping that translation here prevents player state from also
 * becoming responsible for promotion, en-passant and castling reconstruction.</p>
 */
public final class MoveSimulationMapper {

    private MoveSimulationMapper() {
    }

    /**
     * Maps {@code move} to equivalent objects owned by {@code simulation}.
     *
     * @param simulation target game/simulation
     * @param move source move
     * @return equivalent move for the target game, or {@code null} when it cannot be mapped
     */
    public static Move map(Game simulation, Move move) {
        if (simulation == null || move == null || move.getSource() == null || move.getTarget() == null) return null;

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
            return simulatedPromotedPiece != null
                    ? new PromotionImpl(piece, source, target, simulatedPromotedPiece)
                    : null;
        }

        if (move instanceof EnPassant enPassant) {
            Field slayedField = chessBoard.getField(
                    enPassant.getSlayedPiece().getField().getFile(),
                    enPassant.getSlayedPiece().getField().getRank());
            Piece slayedPiece = slayedField.getPiece();
            return slayedPiece instanceof Pawn pawn
                    ? new EnPassantImpl(piece, source, target, pawn)
                    : null;
        }

        if (move instanceof Castling castling) {
            Field rookSource = chessBoard.getField(
                    castling.getRookSource().getFile(),
                    castling.getRookSource().getRank());
            Piece rookPiece = rookSource.getPiece();
            return rookPiece instanceof Rook rook
                    ? new CastlingImpl(piece, rook, castling.getSide())
                    : null;
        }

        return new ChessMove(piece, source, target);
    }
}
