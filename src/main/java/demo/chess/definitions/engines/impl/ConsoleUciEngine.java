package demo.chess.definitions.engines.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.engines.ChessEngine;
import demo.chess.definitions.engines.EngineConfig;

public abstract class ConsoleUciEngine implements ChessEngine {

	protected static final Logger logger = LogManager.getLogger(ConsoleUciEngine.class);

	protected Process uciEngineProcess;
	protected PrintWriter writer;
	protected BufferedReader reader;

	public ConsoleUciEngine(String path) throws Exception {
		uciEngineProcess = new ProcessBuilder(path).start();
		writer = new PrintWriter(new OutputStreamWriter(uciEngineProcess.getOutputStream()), true);
		reader = new BufferedReader(new InputStreamReader(uciEngineProcess.getInputStream()));
	}

	@Override
	public synchronized void close() {
		try {
			if (writer != null) {
				writer.println("quit");
				writer.flush();
				writer.close();
			}
		} catch (Exception e) {
			logger.debug("Could not send quit to UCI engine", e);
		}

		try {
			if (reader != null) {
				reader.close();
			}
		} catch (Exception e) {
			logger.debug("Could not close UCI engine reader", e);
		}

		if (uciEngineProcess != null) {
			uciEngineProcess.destroy();
			try {
				if (!uciEngineProcess.waitFor(1, TimeUnit.SECONDS)) {
					uciEngineProcess.destroyForcibly();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				uciEngineProcess.destroyForcibly();
			}
		}
	}

	protected abstract StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config);

	protected PrintWriter getWriter() {
		return writer;
	}

}
