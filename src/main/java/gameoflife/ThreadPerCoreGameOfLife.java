package gameoflife;

import java.util.ArrayList;
import java.util.List;

import gameoflife.concurrent.BlockingRendezVous;
import gameoflife.domain.CellsGroup;
import gameoflife.domain.Channel;
import gameoflife.domain.Dimensions;

public class ThreadPerCoreGameOfLife extends GameOfLife {

    private final List<CellsGroup> cellsGroups = new ArrayList<>();

    public ThreadPerCoreGameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel,
                                   boolean useVirtualThreads, BlockingRendezVous.Type type) {
        super(dimensions, seed, period, gridChannel, useVirtualThreads, type);

        int nThread = getThreadPoolSize();
        // 1 thread is left out to be used by the frame computation orchestrator
        int cellsPerGroup = (int) Math.round( (double) cells.size() / (double) (nThread-1) );
        int groupStart = 0;
        for (int i = 0; i < nThread-2; i++) {
            cellsGroups.add( new CellsGroup( cells.subList(groupStart, groupStart + cellsPerGroup) ) );
            groupStart += cellsPerGroup;
        }
        cellsGroups.add( new CellsGroup( cells.subList(groupStart, cells.size()) ) );
    }

    private static class LazySettings {

        private static final int PARALLELISM;

        static {
            final int availableCores = Runtime.getRuntime().availableProcessors();
            final Integer fjParallelism = Integer.getInteger("jdk.virtualThreadScheduler.parallelism");
            PARALLELISM = fjParallelism == null || fjParallelism <= 0 ? availableCores : fjParallelism;
            System.out.println(PARALLELISM);
        }
    }

    @Override
    public void startCells() {
        cellsGroups.forEach( cg -> runner.accept(cg::run) );
    }

    @Override
    protected int getThreadPoolSize() {
        return LazySettings.PARALLELISM;
    }
}
