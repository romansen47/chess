package demo.chess.definitions;

import java.util.Arrays;

/**
 * Immutable value object describing one of the 960 legal Chess960 starting positions.
 *
 * <p>The numeric identifier follows Scharnagl numbering. Classical chess is not
 * treated as a separate variant in the domain model; it is position {@value #STANDARD_ID}.
 * All instances are canonical and can be obtained through {@link #of(int)}.</p>
 *
 * <p>Besides the back-rank layout, the object exposes the original king and rook
 * files. Those files are part of the initial-position context required for
 * Chess960 castling, FEN export and UCI communication.</p>
 */
public final class ChessStartingPosition {

    /** Lowest valid Scharnagl position identifier. */
    public static final int MIN_ID = 0;

    /** Highest valid Scharnagl position identifier. */
    public static final int MAX_ID = 959;

    /** Scharnagl identifier of the classical RNBQKBNR position. */
    public static final int STANDARD_ID = 518;

    private static final int[] LIGHT_SQUARE_FILES = {2, 4, 6, 8};
    private static final int[] DARK_SQUARE_FILES = {1, 3, 5, 7};
    private static final int[][] KNIGHT_COMBINATIONS = {
            {0, 1}, {0, 2}, {0, 3}, {0, 4}, {1, 2},
            {1, 3}, {1, 4}, {2, 3}, {2, 4}, {3, 4}
    };
    private static final ChessStartingPosition[] POSITIONS = createPositions();

    /** Canonical classical starting position (Scharnagl 518). */
    public static final ChessStartingPosition STANDARD = POSITIONS[STANDARD_ID];

    private final int id;
    private final PieceType[] backRank;
    private final int kingFile;
    private final int queenSideRookFile;
    private final int kingSideRookFile;

    private ChessStartingPosition(int id) {
        this.id = id;
        this.backRank = decode(id);
        this.kingFile = findFile(PieceType.KING);

        int firstRook = -1;
        int secondRook = -1;
        for (int file = 1; file <= 8; file++) {
            if (backRank[file - 1] == PieceType.ROOK) {
                if (firstRook < 0) firstRook = file;
                else secondRook = file;
            }
        }
        this.queenSideRookFile = Math.min(firstRook, secondRook);
        this.kingSideRookFile = Math.max(firstRook, secondRook);
        if (!(queenSideRookFile < kingFile && kingFile < kingSideRookFile)) {
            throw new IllegalStateException("Decoded Chess960 position violates R-K-R ordering: " + id);
        }
    }

    /**
     * Returns the canonical value object for a Scharnagl identifier.
     *
     * @param id identifier in the inclusive range 0..959
     * @return canonical starting position
     * @throws IllegalArgumentException if the identifier is outside the valid range
     */
    public static ChessStartingPosition of(int id) {
        if (id < MIN_ID || id > MAX_ID) {
            throw new IllegalArgumentException("Chess960 position id must be between 0 and 959: " + id);
        }
        return POSITIONS[id];
    }

    /**
     * Resolves an initial-position FEN to its Chess960 starting position.
     *
     * <p>This method intentionally accepts only complete initial piece placement.
     * It is not a general FEN position parser.</p>
     *
     * @param fen full or placement-only FEN describing a Chess960 initial position
     * @return corresponding canonical starting position
     * @throws IllegalArgumentException if the FEN is empty or is not an initial Chess960 layout
     */
    public static ChessStartingPosition fromInitialFen(String fen) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN must not be empty");
        }
        String placement = fen.trim().split("\\s+")[0];
        for (ChessStartingPosition candidate : POSITIONS) {
            if (candidate.initialPiecePlacement().equals(placement)) return candidate;
        }
        throw new IllegalArgumentException("FEN is not a Chess960 initial position: " + fen);
    }

    public int getId() {
        return id;
    }

    /** @return whether this is classical chess (Scharnagl 518) */
    public boolean isStandard() {
        return id == STANDARD_ID;
    }

    /**
     * Returns the type occupying a back-rank file in the initial position.
     *
     * @param file one-based file number (a=1, h=8)
     * @return initial piece type on that file
     */
    public PieceType getPieceTypeAtFile(int file) {
        if (file < 1 || file > 8) {
            throw new IllegalArgumentException("File must be between 1 and 8: " + file);
        }
        return backRank[file - 1];
    }

    /** @return defensive copy of the eight back-rank piece types */
    public PieceType[] getBackRank() {
        return Arrays.copyOf(backRank, backRank.length);
    }

    public int getKingFile() {
        return kingFile;
    }

    public int getQueenSideRookFile() {
        return queenSideRookFile;
    }

    public int getKingSideRookFile() {
        return kingSideRookFile;
    }

    /**
     * Returns the complete initial FEN.
     *
     * <p>All 960 positions, including Scharnagl 518, use file-based
     * Shredder-FEN castling rights. Position 518 is represented by the same
     * data model and serialization rules as every other starting position.</p>
     */
    public String initialFen() {
        return initialPiecePlacement() + " w " + initialCastlingFen() + " - 0 1";
    }

    /** @return the eight-rank FEN piece-placement field for the initial board */
    public String initialPiecePlacement() {
        String white = backRankFen(true);
        String black = backRankFen(false);
        return black + "/pppppppp/8/8/8/8/PPPPPPPP/" + white;
    }

    /** @return initial castling-rights FEN field */
    public String initialCastlingFen() {
        char whiteKingSide = fileLetter(kingSideRookFile, true);
        char whiteQueenSide = fileLetter(queenSideRookFile, true);
        char blackKingSide = fileLetter(kingSideRookFile, false);
        char blackQueenSide = fileLetter(queenSideRookFile, false);
        return new String(new char[] {
                whiteKingSide, whiteQueenSide, blackKingSide, blackQueenSide
        });
    }

    private String backRankFen(boolean white) {
        StringBuilder value = new StringBuilder(8);
        for (PieceType type : backRank) {
            char piece = switch (type) {
                case ROOK -> 'r';
                case KNIGHT -> 'n';
                case BISHOP -> 'b';
                case QUEEN -> 'q';
                case KING -> 'k';
                case PAWN -> 'p';
            };
            value.append(white ? Character.toUpperCase(piece) : piece);
        }
        return value.toString();
    }

    private static ChessStartingPosition[] createPositions() {
        ChessStartingPosition[] positions = new ChessStartingPosition[MAX_ID + 1];
        for (int id = MIN_ID; id <= MAX_ID; id++) positions[id] = new ChessStartingPosition(id);
        return positions;
    }

    private static PieceType[] decode(int positionId) {
        PieceType[] result = new PieceType[8];
        int n = positionId;

        int lightBishopFile = LIGHT_SQUARE_FILES[n % 4];
        n /= 4;
        int darkBishopFile = DARK_SQUARE_FILES[n % 4];
        n /= 4;
        result[lightBishopFile - 1] = PieceType.BISHOP;
        result[darkBishopFile - 1] = PieceType.BISHOP;

        int[] remaining = remainingFiles(result);
        int queenIndex = n % 6;
        n /= 6;
        result[remaining[queenIndex] - 1] = PieceType.QUEEN;

        remaining = remainingFiles(result);
        int[] knights = KNIGHT_COMBINATIONS[n];
        result[remaining[knights[0]] - 1] = PieceType.KNIGHT;
        result[remaining[knights[1]] - 1] = PieceType.KNIGHT;

        remaining = remainingFiles(result);
        result[remaining[0] - 1] = PieceType.ROOK;
        result[remaining[1] - 1] = PieceType.KING;
        result[remaining[2] - 1] = PieceType.ROOK;
        return result;
    }

    private static int[] remainingFiles(PieceType[] rank) {
        int count = 0;
        for (PieceType type : rank) if (type == null) count++;
        int[] result = new int[count];
        int index = 0;
        for (int file = 1; file <= 8; file++) {
            if (rank[file - 1] == null) result[index++] = file;
        }
        return result;
    }

    private int findFile(PieceType type) {
        for (int file = 1; file <= 8; file++) {
            if (backRank[file - 1] == type) return file;
        }
        throw new IllegalStateException("Missing " + type + " in Chess960 position " + id);
    }

    private static char fileLetter(int file, boolean white) {
        char lower = (char) ('a' + file - 1);
        return white ? Character.toUpperCase(lower) : lower;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof ChessStartingPosition position && id == position.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Chess960-" + id;
    }
}
