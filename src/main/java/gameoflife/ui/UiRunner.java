package gameoflife.ui;

import java.util.function.Consumer;

import gameoflife.ExecutionArgs;
import gameoflife.GameOfLife;

public class UiRunner {

    static final boolean IS_NATIVE_IMAGE = System.getProperty("org.graalvm.nativeimage.imagecode") != null;

    public static void runUI(ExecutionArgs args, GameOfLife gameOfLife, boolean graphical) {
        Consumer<boolean[][]> output = !IS_NATIVE_IMAGE && graphical ? new WindowOutput(args, gameOfLife) : new ConsoleOutput(args, gameOfLife);
        while (true) {
            output.accept(gameOfLife.getGridChannel().take());
        }
    }
}
