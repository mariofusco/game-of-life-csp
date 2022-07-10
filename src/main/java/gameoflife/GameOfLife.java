package gameoflife;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import gameoflife.domain.Cell;
import gameoflife.domain.CellOptions;
import gameoflife.domain.Channel;
import gameoflife.domain.Dimensions;
import gameoflife.ui.PatternParser;

public abstract class GameOfLife {

    private final Dimensions dimensions;
    private final int period;
    private final boolean logRate;
    private final Channel<boolean[][]> gridChannel;
    private final boolean[][] grid;
    protected final List<Cell> cells;
    private final List<List<Channel<Boolean>>> tickChannels;
    private final List<List<Channel<Boolean>>> resultChannels;

    private long lastStatsDump = System.nanoTime();
    private long framesCount = 0;

    protected final Consumer<Runnable> runner;

    public GameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel,
                      boolean logRate, boolean useVirtualThreads) {
        this.dimensions = dimensions;
        this.grid = new boolean[dimensions.rows()][dimensions.cols()];
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

        if (useVirtualThreads) {
            this.runner = Thread::startVirtualThread;
        } else {
            this.runner = Executors.newFixedThreadPool(getThreadPoolSize(), r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            })::execute;
        }
    }

    public static GameOfLife create(ExecutionArgs args) {
        boolean[][] original;
        try {
            original = PatternParser.parseFile(args.patternFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean[][] rotated = args.rotate() ? PatternParser.rotate(original) : original;
        boolean[][] pattern = PatternParser.pad(rotated, args.leftPadding(), args.topPadding(), args.rightPadding(), args.bottomPadding());

        Channel<boolean[][]> gridChannel = new Channel<>(); // channel carries aggregated liveness matrices
        Dimensions dimensions = new Dimensions(pattern.length, pattern[0].length, args.toroidal());
        return GameOfLife.create(args, dimensions, pattern, gridChannel);
    }

    private static GameOfLife create(ExecutionArgs args, Dimensions dimensions, boolean[][] seed, Channel<boolean[][]> gridChannel) {
        return args.threadPerCell() ?
                new ThreadPerCellGameOfLife(dimensions, seed, args.periodMilliseconds(), gridChannel, args.logRate(), args.useVirtualThreads()) :
                new ThreadPerCoreGameOfLife(dimensions, seed, args.periodMilliseconds(), gridChannel, args.logRate(), args.useVirtualThreads());
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

    protected abstract int getThreadPoolSize();

    public abstract void startCells();

    public void startGame() {
        runner.accept(this::run);
    }

    private void run() {
        while (true) {
            calculateFrame();
        }
    }

    public boolean[][] calculateFrame() {
        dimensions.forEachRowCol((r, c) -> tickChannels.get(r).get(c).put(true)); // emit tick event for every cell
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
