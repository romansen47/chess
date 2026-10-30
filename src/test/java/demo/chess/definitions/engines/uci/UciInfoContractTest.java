package demo.chess.definitions.engines.uci;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class UciInfoContractTest {

    @Test
    public void parserConformsToSharedBrowserAndJavaTranscriptContract() throws IOException {
        for (ContractCase testCase : loadContract()) {
            Optional<UciInfoLine> parsed =
                    UciInfoParser.parsePrincipalVariation(testCase.line());
            if (!testCase.usable()) {
                assertTrue(testCase.name(), parsed.isEmpty());
                continue;
            }

            UciInfoLine line = parsed.orElseThrow();
            assertEquals(testCase.name(), testCase.depth(), Integer.valueOf(line.depth()));
            assertEquals(testCase.name(), testCase.multiPv(), Integer.valueOf(line.multiPv()));
            assertEquals(testCase.name(), testCase.centipawns(), line.centipawnScore());
            assertEquals(testCase.name(), testCase.mate(), line.mateScore());
            assertEquals(testCase.name(), testCase.pv(), line.pv());
            double rawEvaluation = line.isMateScore()
                    ? Integer.signum(line.mateScore()) * 99d
                    : line.centipawnScore() / 100d;
            assertEquals(testCase.name(), testCase.whiteEvaluation(), rawEvaluation, 0.0d);
            assertEquals(testCase.name(), testCase.blackEvaluation(), -rawEvaluation, 0.0d);
        }
    }

    private List<ContractCase> loadContract() throws IOException {
        Path path = Path.of("contracts", "uci-info-contract.tsv");
        if (!Files.isRegularFile(path)) {
            path = Path.of("..", "contracts", "uci-info-contract.tsv");
        }
        return Files.readAllLines(path).stream()
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(ContractCase::parse)
                .toList();
    }

    private record ContractCase(
            String name,
            boolean usable,
            Integer depth,
            Integer multiPv,
            Integer centipawns,
            Integer mate,
            String pv,
            Double whiteEvaluation,
            Double blackEvaluation,
            String line) {

        private static ContractCase parse(String row) {
            String[] fields = row.split("\\t", -1);
            return new ContractCase(
                    fields[0],
                    Boolean.parseBoolean(fields[1]),
                    integer(fields[2]),
                    integer(fields[3]),
                    integer(fields[4]),
                    integer(fields[5]),
                    text(fields[6]),
                    decimal(fields[7]),
                    decimal(fields[8]),
                    fields[10]);
        }

        private static Integer integer(String value) {
            return "-".equals(value) ? null : Integer.valueOf(value);
        }

        private static Double decimal(String value) {
            return "-".equals(value) ? null : Double.valueOf(value);
        }

        private static String text(String value) {
            return "-".equals(value) ? null : value;
        }
    }
}
