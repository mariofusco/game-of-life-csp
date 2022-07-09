package gameoflife;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public abstract class GameOfLife {

    private final Dimensions dimensions;
    private final int period;
    private final boolean logRate;
    private final Channel<boolean[][]> gridChannel;
    protected final List<Cell> cells;
    private final List<List<Channel<Boolean>>> tickChannels;
    private final List<List<Channel<Boolean>>> resultChannels;

    private long lastStatsDump = System.nanoTime();
    private long framesCount = 0;

    GameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel, boolean logRate) {
        this.dimensions = dimensions;
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

    public static GameOfLife create(ExecutionArgs a) {
        boolean[][] original = null;
        try {
            original = PatternParser.parseFile(a.patternFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean[][] rotated = a.rotate() ? PatternParser.rotate(original) : original;
        boolean[][] pattern = PatternParser.pad(rotated, a.leftPadding(), a.topPadding(), a.rightPadding(), a.bottomPadding());

        Channel<boolean[][]> gridChannel = new Channel<>(); // channel carries aggregated liveness matrices
        Dimensions dimensions = new Dimensions(pattern.length, pattern[0].length, a.toroidal());
        return GameOfLife.create(a.useVirtualThreads(), dimensions, pattern, a.periodMilliseconds(), gridChannel, a.logRate());
    }

    static GameOfLife create(boolean virtual, Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel, boolean logRate) {
        return virtual ?
                new VirtualGameOfLife(dimensions, seed, period, gridChannel, logRate) :
                new NativeGameOfLife(dimensions, seed, period, gridChannel, logRate);
    }

    Channel<boolean[][]> getGridChannel() {
        return gridChannel;
    }

    Dimensions getDimensions() {
        return dimensions;
    }

    public void start() {
        startCells();
        startGame();
    }

    public abstract void startCells();
    public abstract void startGame();

    protected void run() {
        while (true) {
            calculateFrame();
        }
    }

    public boolean[][] calculateFrame() {
        dimensions.forEachRowCol((r, c) -> tickChannels.get(r).get(c).put(true)); // emit tick event for every cell
        boolean[][] grid = new boolean[dimensions.rows()][dimensions.cols()]; // prepare result boolean matrix
        dimensions.forEachRowCol((r, c) -> grid[r][c] = resultChannels.get(r).get(c).take()); // populate matrix with results
        gridChannel.put(grid); // emit aggregated liveness matrix
        endOfFrame();
        return grid;
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
