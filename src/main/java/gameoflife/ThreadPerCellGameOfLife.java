package gameoflife;

import gameoflife.domain.Channel;
import gameoflife.domain.Dimensions;

public class ThreadPerCellGameOfLife extends GameOfLife {

    public ThreadPerCellGameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<Boolean[][]> gridChannel,
                                   boolean logRate, boolean useVirtualThreads) {
        super(dimensions, seed, period, gridChannel, logRate, useVirtualThreads);
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
