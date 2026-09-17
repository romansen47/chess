package demo.chess.admin.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import demo.chess.admin.Admin;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.board.impl.ChessBoard;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.impl.MoveListImpl;
import demo.chess.definitions.players.impl.BlackPlayerImpl;
import demo.chess.definitions.players.impl.WhitePlayerImpl;
import demo.chess.game.Game;
import demo.chess.game.impl.ChessGame;

/** Configuration class providing chess games. */
@Configuration
public class ChessAdmin implements Admin {

    @Override
    @Bean
    @Scope("prototype")
    public Game chessGame(int time) {
        return chessGame(time, ChessStartingPosition.STANDARD);
    }

    @Override
    public Game chessGame(int time, ChessStartingPosition startingPosition) {
        MoveList moveList = new MoveListImpl();
        moveList.setStartingPosition(startingPosition);
        return new ChessGame(
                new ChessBoard(),
                new WhitePlayerImpl(moveList, "ChessGame"),
                new BlackPlayerImpl(moveList, "ChessGame"),
                moveList,
                this,
                time,
                startingPosition);
    }
}
