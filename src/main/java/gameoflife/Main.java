package gameoflife;

import java.io.IOException;
import java.util.function.Consumer;

import gameoflife.domain.Dimensions;
import gameoflife.ui.UiRunner;
import gameoflife.ui.WindowOutput;

public class Main {

    public static void main(String[] args) throws IOException {
        execute(ExecutionArgs.parse(args));
    }

    public static void execute(ExecutionArgs args) throws IOException {
        GameOfLife gameOfLife = createGameOfLife(args);
        UiRunner.runUI(args, gameOfLife, true);
    }

    public static void executeHeadless(ExecutionArgs args) throws IOException {
        GameOfLife gameOfLife = createGameOfLife(args);

        while (true) {
            gameOfLife.calculateFrameBlocking();
        }

//        Thread.startVirtualThread( () -> {
//            while (true) {
//                gameOfLife.calculateFrameBlocking();
//            }
//        });
//
//        System.in.read();
    }

    private static GameOfLife createGameOfLife(ExecutionArgs args) {
        GameOfLife gameOfLife = GameOfLife.create(args);
        gameOfLife.start();

        System.out.println(args);
        System.out.println(gameOfLife.getDimensions());
        return gameOfLife;
    }
}
