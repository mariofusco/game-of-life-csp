package gameoflife;

import gameoflife.domain.Channel;
import gameoflife.domain.Dimensions;

public class ThreadPerCellGameOfLife extends GameOfLife {

    public ThreadPerCellGameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel,
                                   boolean logRate, boolean useNativeThreads) {
        super(dimensions, seed, period, gridChannel, logRate, useNativeThreads);
    }

    @Override
    public void startCells() {
        cells.forEach(cell -> runner.accept(cell::run));
    }

    @Override
    protected int getThreadPoolSize() {
        return cells.size()+1;
    }
}
