package gameoflife;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeGameOfLife extends GameOfLife {

    private final int nThread = Runtime.getRuntime().availableProcessors();

    private final ExecutorService executor = Executors.newFixedThreadPool(nThread);

    private final List<CellsGroup> cellsGroups = new ArrayList<>();

    NativeGameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel, boolean logRate) {
        super(dimensions, seed, period, gridChannel, logRate);

        int cellsPerGroup = (int) Math.round( (double) cells.size() / (double) (nThread-1) );
        int groupStart = 0;
        for (int i = 0; i < nThread-2; i++) {
            cellsGroups.add( new CellsGroup( cells.subList(groupStart, groupStart + cellsPerGroup) ) );
            groupStart += cellsPerGroup;
        }
        cellsGroups.add( new CellsGroup( cells.subList(groupStart, cells.size()) ) );
    }

    @Override
    public void start() {
        cellsGroups.forEach( cg -> executor.submit(cg::run) );
        executor.submit(this::run);
    }
}
