package gameoflife;

import gameoflife.concurrent.BlockingRendezVous;
import gameoflife.domain.Channel;
import gameoflife.domain.Dimensions;

public class ThreadPerCellGameOfLife extends GameOfLife {

    public ThreadPerCellGameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel,
                                   boolean logRate, boolean useVirtualThreads, BlockingRendezVous.Type type) {
        super(dimensions, seed, period, gridChannel, logRate, useVirtualThreads, type);
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
