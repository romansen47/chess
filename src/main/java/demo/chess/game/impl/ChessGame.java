package demo.chess.game.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

import demo.chess.admin.Admin;
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
import demo.chess.definitions.players.BlackPlayer;
import demo.chess.definitions.players.WhitePlayer;
import demo.chess.game.Game;

/**
 * The ChessGame class implements the core functionality for applying different
 * types of chess moves.
 */
public class ChessGame extends ChessGameTemplate {

	private Admin admin;

	final int timeForEachPlayer;

	int incrementForWhite;

	int incrementForBlack;

	private List<String> sanMoveList = new ArrayList<>();

	/**
	 * Constructs a ChessGame instance with the given chessboard, white player,
	 * black player, and move list.
	 *
	 * @param chessBoard  the chess board
	 * @param whitePlayer the white player
	 * @param blackPlayer the black player
	 * @param moveList    the list of moves
	 * @param chessAdmin
	 * @throws Exception
	 */
	public ChessGame(Board chessBoard, WhitePlayer whitePlayer, BlackPlayer blackPlayer, MoveList moveList,
			Admin chessAdmin, int timeForEachPlayer) throws Exception {
		super(chessBoard, whitePlayer, blackPlayer, moveList);
		LogManager.getLogger().info("creating new Game. time: {}", timeForEachPlayer);
		this.setAdmin(chessAdmin);
		this.timeForEachPlayer = timeForEachPlayer;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(Admin admin) {
		this.admin = admin;
	}

	/**
	 * Switches the current player to the other player.
	 */
	@Override
	public void switchPlayer() {
		if (!getPlayer().getChessClock().isStarted()) {
			getPlayer().getChessClock().start();
		}
		getPlayer().getChessClock().addIncrement();
		getPlayer().getChessClock().suspend();
		super.switchPlayer();
		if (!getPlayer().getChessClock().isStarted()) {
			getPlayer().getChessClock().start();
		} else {
			getPlayer().getChessClock().resume();
		}
	}

	@Override
	public int getTimeForEachPlayer() {
		return timeForEachPlayer;
	}

	@Override
	public int getIncrementForWhite() {
		return incrementForWhite;
	}

	@Override
	public void setIncrementForWhite(int incrementForWhite) {
		this.incrementForWhite = incrementForWhite;
	}

	@Override
	public int getIncrementForBlack() {
		return incrementForBlack;
	}

	@Override
	public void setIncrementForBlack(int incrementForBlack) {
		this.incrementForBlack = incrementForBlack;
	}

	@Override
	public List<String> getSanMoveList() {
		return sanMoveList;
	}

	@Override
	public void setSanMoveList(List<String> sanMoveList) {
		this.sanMoveList = sanMoveList;
	}

	/**
	 * Applies the given move to the chess game and computes actual uciEngine
	 * evaluation.
	 *
	 * @param move the move to apply
	 * @throws IOException
	 * @throws NoMoveFoundException
	 */
	@Override
	public void apply(Move move) throws NoMoveFoundException, IOException {
		sanMoveList.add(getShortAlgebraicNotatedMove(move));
		super.apply(move);
	}

	public String getUnicodeSymbol(String s, Color color) { 
			switch (color) {
			case WHITE:
				switch (s.toLowerCase()) {
				case "k":
					return "♔";
				case "q":
					return "♕";
				case "r":
					return "♖";
				case "b":
					return "♗";
				case "n":
					return "♘"; 
				}
			case BLACK:
				switch (s.toLowerCase()) {
				case "k":
					return "♚";
				case "q":
					return "♛";
				case "r":
					return "♜";
				case "b":
					return "♝";
				case "n":
					return "♞";
			}
			default:
				return "";
			}
	}
	
	public String getShortAlgebraicNotatedMove(Move moveToExecute) throws NoMoveFoundException, IOException {
		Game simulation = getAdmin().simulation();
		for (Move move : getMoveList()) {
			simulation.apply(simulation.getPlayer().getMoveInSimulation(simulation, move));
		}
		String convertedMove = "";
		Field originalSource = moveToExecute.getSource();
		Field originalTarget = moveToExecute.getTarget();
		Field source = simulation.getChessBoard().getField(originalSource.getFile(), originalSource.getRank());
		Field target = simulation.getChessBoard().getField(originalTarget.getFile(), originalTarget.getRank());
		Move moveInSimulation = simulation.getPlayer().getMoveInSimulation(simulation, moveToExecute);
		String pieceToString = getUnicodeSymbol(getPiecePrefix(source.getPiece()),source.getPiece().getColor());
		String sourceFieldToString = "";
		if (moveInSimulation.getPiece().getType().equals(PieceType.PAWN) && target.getPiece() != null) {
			sourceFieldToString = moveInSimulation.getPiece().getField().toString().substring(0, 1);
		}
		String hits = target.getPiece() == null || moveToExecute instanceof Castling ? "" : "x";
		String targetFieldToString = target.toString();
		String postFix = moveInSimulation instanceof EnPassant ? " e.p." : "";

		if (moveInSimulation instanceof EnPassant) {
			sourceFieldToString = moveInSimulation.getPiece().getField().toString().substring(0, 1);
			hits = "x";
		}
		List<Move> validMoves = simulation.getPlayer().getValidMoves(this);

		if (moveInSimulation instanceof Castling) {
			pieceToString = "";
			sourceFieldToString = "";
			targetFieldToString = "";
			postFix = "0-0-0";
			if (source.getFile() > 4) {
				postFix = "0-0";
			}
		} else if (moveInSimulation instanceof Promotion) {
			targetFieldToString = target.toString();
			List<Promotion> promotions = new ArrayList<>();
			validMoves.forEach(move -> {
				if (move instanceof Promotion && move.getTarget().equals(target)) {
					promotions.add(((Promotion) move));
				}
			});
			if (promotions.size() != 4) {
				sourceFieldToString = source.getName();
			}
			postFix = "=" + getPiecePrefix(((Promotion) moveInSimulation).getPromotedPiece());

		} else {
			List<Piece> possiblePieces = new ArrayList<>();
			validMoves.forEach(move -> {
				if (move.getTarget().equals(target)) {
					possiblePieces.add(move.getPiece());
				}
			});
			if (possiblePieces.size() != 1) {
				List<String> s = new ArrayList<>();
				String relevantType = getPiecePrefix(source.getPiece());
				possiblePieces.forEach(piece -> {
					if (getPiecePrefix(piece).equals(relevantType) && piece != moveInSimulation.getPiece()) {
						s.add(relevantType);
					}
				});
				if (s.size() > 0) {
					if (s.size() > 1) {
						sourceFieldToString = source.toString();
					} else {
						if (s.size() == 1) {
							if (moveInSimulation.getSource().getFile() != possiblePieces.get(0).getField().getFile()) {
								sourceFieldToString = moveInSimulation.getSource().toString().substring(0,1);
							} else {
								sourceFieldToString = moveInSimulation.getSource().toString().substring(1,2);
							}
						}
					}
				}
			}
		}
		convertedMove = pieceToString + sourceFieldToString + hits + targetFieldToString + postFix;
		return convertedMove;
	}

	String getPiecePrefix(Piece piece) {
		if (piece.getType().equals(PieceType.PAWN)) {
			return StringUtils.EMPTY;
		}
		return piece.getType().equals(PieceType.KNIGHT) ? "N" : piece.getType().name().substring(0, 1);
	}
}
