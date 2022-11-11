package gameoflife.ui;

import java.util.function.Consumer;

import gameoflife.ExecutionArgs;
import gameoflife.GameOfLife;
import gameoflife.domain.Dimensions;

import static gameoflife.ExecutionArgs.IS_NATIVE_IMAGE;

public class ConsoleOutput implements Consumer<boolean[][]> {

    private final int MAX_ROWS = 50;
    private final int MAX_COLS = 200 / 2; /* 2 chars per column */

    private final RateCounter rateCounter = new RateCounter();
    private final String statusBarTemplate;

    ConsoleOutput(ExecutionArgs args, GameOfLife gameOfLife) {
        String mode = IS_NATIVE_IMAGE ? "AOT" : "JIT";
        Dimensions dimensions = gameOfLife.getDimensions();
        statusBarTemplate = String.format("%n GraalVM %s mode | %d cols and %d rows | %%d frames/second%%n%n", mode, dimensions.cols(), dimensions.rows());
        ANSI.hideCursor();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ANSI.showCursor();
            ANSI.clear();
            System.out.flush();
        }));
    }

    @Override
    public void accept(boolean[][] cells) {
        System.out.printf(statusBarTemplate, rateCounter.tick());
        ANSI.bold();
        int rows = Math.min(cells.length, MAX_ROWS);
        for (int r = 0; r < rows; r++) {
            int cols = Math.min(cells[r].length, MAX_COLS);
            for (int c = 0; c < cols; c++) {
                System.out.print(cells[r][c] ? "██" : "░░");
            }
            System.out.println();
        }
        ANSI.reset();
        ANSI.moveTerminalCursorUp(3 + rows);
        System.out.flush();
    }

    static class RateCounter {
        long lastStatsDump;
        int counter;
        int ticksPerSecond;

        int tick() {
            long now = System.nanoTime();
            if (now - lastStatsDump >= 1_000_000_000) {
                lastStatsDump = now;
                ticksPerSecond = counter;
                counter = 1;
            } else {
                counter++;
            }
            return ticksPerSecond;
        }
    }


    private static class ANSI {
        private static void bold() {
            System.out.print("\033[1m");
        }

        private static void reset() {
            System.out.print("\033[00m");
        }

        private static void clear() {
            System.out.print("\033c");
        }

        private static void hideCursor() {
            System.out.print("\033[?25l");
        }

        private static void showCursor() {
            System.out.print("\033[?25h");
        }

        private static void moveTerminalCursorUp(int nLines) {
            System.out.print("\033[" + nLines + "A");
        }
    }
}
