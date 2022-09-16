package gameoflife;

import java.util.ArrayList;
import java.util.List;

import gameoflife.concurrent.BlockingRendezVous;
import gameoflife.domain.CellsGroup;
import gameoflife.domain.Channel;
import gameoflife.domain.Dimensions;

public class ThreadPerCoreGameOfLife extends GameOfLife {

    private final List<CellsGroup> cellsGroups;

    public ThreadPerCoreGameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel,
                                   boolean logRate, boolean useVirtualThreads, BlockingRendezVous.Type type) {
        super(dimensions, seed, period, gridChannel, logRate, useVirtualThreads, type);
        cellsGroups = createCellGroups();
    }

    private List<CellsGroup> createCellGroups() {
        List<CellsGroup> cellsGroups = new ArrayList<>();
        int nThread = Runtime.getRuntime().availableProcessors();
        int cellsPerGroup = (int) Math.round( (double) cells.size() / (double) (nThread-1) );
        int groupStart = 0;
        for (int i = 0; i < nThread-2; i++) {
            cellsGroups.add( new CellsGroup( cells.subList(groupStart, groupStart + cellsPerGroup) ) );
            groupStart += cellsPerGroup;
        }
        cellsGroups.add( new CellsGroup( cells.subList(groupStart, cells.size()) ) );
        return cellsGroups;
    }

    @Override
    public void startCells() {
        cellsGroups.forEach( cg -> runner.accept(cg::run) );
    }

    @Override
    protected int getThreadPoolSize() {
        return Runtime.getRuntime().availableProcessors();
    }
}
