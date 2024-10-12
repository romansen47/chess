package demo.chess.save;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;

public class GameSaver {

	public void saveGame(MoveList moveList, String location) throws IOException {
		// Versuch, die Datei zu schreiben
		FileWriter fileWriter = new FileWriter(location);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

		String s = "";
		for (Move move : moveList) {
			s = move.toString();
			// Schreiben des Inhalts in die Datei
			bufferedWriter.write(s);
			bufferedWriter.newLine();
			bufferedWriter.flush();
		}
		System.out.println("Datei erfolgreich geschrieben!");
		bufferedWriter.close();
		fileWriter.close();

	}
}
