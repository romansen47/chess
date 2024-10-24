package demo.chess.definitions.moves.test;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;

import demo.chess.admin.Admin;
import demo.chess.definitions.moves.TestClass;
import demo.chess.game.Game;
import demo.chess.init.Initializer;
import demo.chess.load.GameLoader;

public abstract class TestClassAbstract implements TestClass {

	Logger logger;
	protected static ApplicationContext applicationContext;
	private static Admin admin;

	protected final GameLoader loader = new GameLoader();
	private Game chessGame;

	/**
	 * Create the annotation application context
	 *
	 * @throws Exception
	 */
	@BeforeClass
	public static void prepare() throws Exception {
		final var ctx = (LoggerContext) LogManager.getContext(false);
		final var config = ctx.getConfiguration();
		final var loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		applicationContext = new Initializer().getApplicationContext();
		loggerConfig.setLevel(Level.DEBUG);
		setAdmin((Admin) applicationContext.getBean("chessAdmin"));
//		admin.evaluationEngine().setDepth(1);
	}

	@Before
	public void prepareForEach() throws IOException, Exception {
		setChessGame(getAdmin().chessGame(10));
		loader.loadGame(getPath(), getChessGame());
	}

	/**
	 * @return the path
	 */
	public abstract String getPath();

	public Logger getLogger() {
		logger = logger == null ? logger = LogManager.getLogger(getClass()) : logger;
		return logger;
	}

	/**
	 * @return the admin
	 */
	public static Admin getAdmin() {
		return admin;
	}

	/**
	 * @param admin the admin to set
	 */
	public static void setAdmin(Admin admin) {
		TestClassAbstract.admin = admin;
	}

	/**
	 * @return the chessGame
	 */
	@Override
	public Game getChessGame() {
		return chessGame;
	}

	/**
	 * @param chessGame the chessGame to set
	 */
	protected void setChessGame(Game chessGame) {
		this.chessGame = chessGame;
	}

}
