package gameoflife.ui;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import gameoflife.ExecutionArgs;
import gameoflife.GameOfLife;

public class UiRunner {

    public enum Type {
        Graphical(WindowOutput::new),
        Textual(ConsoleOutput::new),
        Counter((args, gameOfLife) -> new CountingOutput());

        private BiFunction<ExecutionArgs, GameOfLife, Consumer<boolean[][]>> creator;

        Type(BiFunction<ExecutionArgs, GameOfLife, Consumer<boolean[][]>> creator) {
            this.creator = creator;
        }

        private Consumer<boolean[][]> create(ExecutionArgs args, GameOfLife gameOfLife) {
            return creator.apply(args, gameOfLife);
        }
    }

    public static void runUI(ExecutionArgs args, GameOfLife gameOfLife) {
        Consumer<boolean[][]> output = args.uiType().create(args, gameOfLife);
        while (true) {
            output.accept(gameOfLife.getGridChannel().take());
        }
    }
}
