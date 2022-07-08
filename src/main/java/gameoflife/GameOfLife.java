package gameoflife;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public abstract class GameOfLife {

    private final Dimensions dims;
    private final int period;
    private final boolean logRate;
    private final Channel<boolean[][]> gridChannel;
    protected final List<Cell> cells;
    private final List<List<Channel<Boolean>>> tickChannels;
    private final List<List<Channel<Boolean>>> resultChannels;

    private long lastStatsDump = System.nanoTime();
    private long framesCount = 0;

    GameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel, boolean logRate) {
        this.dims = dimensions;
        this.gridChannel = gridChannel;
        this.period = period;
        this.logRate = logRate;
        this.cells = new ArrayList<>();
        this.tickChannels = makeGrid(dimensions.rows(), dimensions.cols(), Channel::new);
        this.resultChannels = makeGrid(dimensions.rows(), dimensions.cols(), Channel::new);

        CellOptions[][] grid = new CellOptions[dimensions.rows()][dimensions.cols()];

        dimensions.forEachRowCol((r, c) -> grid[r][c] = new CellOptions(
                r,
                c,
                seed[r][c],
                tickChannels.get(r).get(c),
                resultChannels.get(r).get(c),
                new ArrayList<>(),
                new ArrayList<>()));

        dimensions.forEachRowCol((r, c) -> {
            CellOptions cell = grid[r][c];
            dimensions.forEachNeighbor(r, c, (ri, ci) -> {
                CellOptions other = grid[ri][ci];
                Channel<Boolean> ch = new Channel<>();
                cell.inChannels().add(ch);
                other.outChannels().add(ch);
            });
        });

        dimensions.forEachRowCol((r, c) -> cells.add(new Cell(grid[r][c])));
    }

    static GameOfLife create(boolean virtual, Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel, boolean logRate) {
        return virtual ?
                new VirtualGameOfLife(dimensions, seed, period, gridChannel, logRate) :
                new NativeGameOfLife(dimensions, seed, period, gridChannel, logRate);
    }

    public abstract void start();

    protected void run() {
        while (true) {
            dims.forEachRowCol((r, c) -> tickChannels.get(r).get(c).put(true)); // emit tick event for every cell
            boolean[][] grid = new boolean[dims.rows()][dims.cols()]; // prepare result boolean matrix
            dims.forEachRowCol((r, c) -> grid[r][c] = resultChannels.get(r).get(c).take()); // populate matrix with results
            gridChannel.put(grid); // emit aggregated liveness matrix
            endOfFrame();
        }
    }

    private void endOfFrame() {
        if (period > 0) {
            try {
                Thread.sleep(period);
            } catch (InterruptedException ignore) { }
        }
        if (logRate) {
            framesCount++;
            if (System.nanoTime() - lastStatsDump >= 1_000_000_000) {
                System.out.printf("Frames per second: %d\n", framesCount);
                lastStatsDump = System.nanoTime();
                framesCount = 0;
            }
        }
    }

    private static <T> List<List<T>> makeGrid(int rows, int cols, Supplier<T> supplier) {
        return IntStream.range(0, rows)
                .mapToObj(r -> IntStream.range(0, cols).mapToObj(c -> supplier.get()).toList())
                .toList();
    }
}
