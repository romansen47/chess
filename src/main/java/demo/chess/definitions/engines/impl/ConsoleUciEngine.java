package demo.chess.definitions.engines.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

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
	public void close() {
		writer.println("quit");
		writer.flush();
		writer.close();
		try {
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		uciEngineProcess.destroy();
	}

	protected abstract StringBuilder getCommandLineOptions(StringBuilder command, EngineConfig config);

}
