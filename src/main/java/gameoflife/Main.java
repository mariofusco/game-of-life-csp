package gameoflife;

import java.io.IOException;
import java.util.function.Consumer;

import gameoflife.domain.Dimensions;
import gameoflife.ui.WindowOutput;

public class Main {

    public static void main(String[] args) throws IOException {
        execute(ExecutionArgs.parse(args));
    }

    public static void execute(ExecutionArgs args) throws IOException {
        GameOfLife gameOfLife = GameOfLife.create(args);
        gameOfLife.start();

        System.out.println(args);
        System.out.println(gameOfLife.getDimensions());
        WindowOutput.runUI(args, gameOfLife);
    }
}
