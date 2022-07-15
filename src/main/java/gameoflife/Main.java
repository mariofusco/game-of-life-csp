package gameoflife;

import java.io.IOException;
import java.util.function.Consumer;

import gameoflife.domain.Dimensions;
import gameoflife.ui.WindowOutput;

public class Main {

    public static void main(String[] args) throws IOException {
        execute(ExecutionArgs.parse(args));
    }

    public static void execute(ExecutionArgs a) throws IOException {
        GameOfLife gameOfLife = GameOfLife.create(a);
        gameOfLife.start();
        runUI(a, gameOfLife);
    }

    private static void runUI(ExecutionArgs args, GameOfLife gameOfLife) {
        Dimensions dimensions = gameOfLife.getDimensions();
        double scale = calculateScale(dimensions.rows(), dimensions.cols(), args.maxWindowWidth(), args.maxWindowHeight());
        var width = (int) (scale * dimensions.cols());
        var height = (int) (scale * dimensions.rows());

        System.out.println(args);
        System.out.println(dimensions);

        Consumer<Boolean[][]> consumer = new WindowOutput(width, height);

        while (true) {
            consumer.accept(gameOfLife.getGridChannel().take());
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
