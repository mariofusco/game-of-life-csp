package gameoflife;

import java.io.IOException;
import java.util.function.Consumer;

public class Main {

    private static final boolean USE_VIRTUAL_THREADS = true;

    private static final String DEFAULT_PATTERN = "patterns/gosper_glider_gun.txt";
    private static final int DEFAULT_MAX_WINDOW_WIDTH = 1280;
    private static final int DEFAULT_MAX_WINDOW_HEIGHT = 960;
    private static final int DEFAULT_PERIOD_MILLISECONDS = 0;
    private static final boolean DEFAULT_LOG_RATE = true;

    record Args(
            String patternFile,
            int maxWindowWidth,
            int maxWindowHeight,
            int periodMilliseconds,
            int leftPadding,
            int topPadding,
            int rightPadding,
            int bottomPadding,
            boolean rotate,
            boolean logRate,
            boolean useVirtualThreads) {
        static Args parse(String[] args) {
            return new Args(
                    args.length > 0 ? args[0] : DEFAULT_PATTERN,
                    args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_MAX_WINDOW_WIDTH,
                    args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_MAX_WINDOW_HEIGHT,
                    args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_PERIOD_MILLISECONDS,
                    args.length > 4 ? Integer.parseInt(args[4]) : 10,
                    args.length > 5 ? Integer.parseInt(args[5]) : 10,
                    args.length > 6 ? Integer.parseInt(args[6]) : 40,
                    args.length > 7 ? Integer.parseInt(args[7]) : 40,
                    args.length > 8 ? Boolean.parseBoolean(args[8]) : false,
                    args.length > 9 ? Boolean.parseBoolean(args[9]) : DEFAULT_LOG_RATE,
                    args.length > 9 ? Boolean.parseBoolean(args[10]) : USE_VIRTUAL_THREADS);
        }
    }

    public static void main(String[] args) throws IOException {
        Args a = Args.parse(args);

        boolean[][] original = PatternParser.parseFile(a.patternFile);
        boolean[][] rotated = a.rotate ? PatternParser.rotate(original) : original;
        boolean[][] pattern = PatternParser.pad(rotated, a.leftPadding, a.topPadding, a.rightPadding, a.bottomPadding);

        Channel<boolean[][]> gridChannel = new Channel<>(); // channel carries aggregated liveness matrices
        Dimensions dimensions = new Dimensions(pattern.length, pattern[0].length);
        GameOfLife.create(a.useVirtualThreads, dimensions, pattern, a.periodMilliseconds, gridChannel, a.logRate).start();

        double scale = calculateScale(dimensions.rows(), dimensions.cols(), a.maxWindowWidth, a.maxWindowHeight);
        var width = (int) (scale * dimensions.cols());
        var height = (int) (scale * dimensions.rows());
        System.out.printf("cells=%d, rows=%d, columns=%d, width=%d, height=%d\n", dimensions.rows()*dimensions.cols(), dimensions.rows(), dimensions.cols(), width, height);
        Consumer<boolean[][]> consumer = new WindowOutput(width, height);

        while (true) {
            consumer.accept(gridChannel.take());
        }
    }

    private static double calculateScale(int rows, int cols, int maxWindowWidth, int maxWindowHeight) {
        double aspect = (double) maxWindowWidth / maxWindowHeight;
        double actual = (double) cols / rows;
        return actual < aspect
                ? (double) maxWindowHeight / rows
                : (double) maxWindowWidth / cols;
    }

}
