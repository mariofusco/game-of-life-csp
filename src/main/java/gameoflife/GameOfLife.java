package gameoflife;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import gameoflife.domain.Cell;
import gameoflife.domain.Channel;
import gameoflife.domain.ChannelsGrid;
import gameoflife.domain.Dimensions;
import gameoflife.domain.Tick;
import gameoflife.domain.TickPerCell;
import gameoflife.ui.PatternParser;

import static gameoflife.domain.ChannelsGrid.makeGrid;

public abstract class GameOfLife {

    protected final List<Cell> cells = new ArrayList<>();

    private final Dimensions dimensions;
    private final int period;
    private final boolean logRate;
    private final Channel<Boolean[][]> gridChannel;
    private final Boolean[][] grid;

    private final Tick tick;
    private final ChannelsGrid<Boolean> resultChannels;

    private long lastStatsDump = System.nanoTime();
    private long framesCount = 0;

    protected final Consumer<Runnable> runner;

    public GameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<Boolean[][]> gridChannel,
                      boolean logRate, boolean useVirtualThreads) {
        this.dimensions = dimensions;
        this.grid = new Boolean[dimensions.rows()][dimensions.cols()];
        this.gridChannel = gridChannel;
        this.period = period;
        this.logRate = logRate;
        this.resultChannels = makeGrid(dimensions);

// TODO: The tick mechanism is now pluggable but for now I'm keeping the original one sending one tick per cell.
//       In reality when no delay is required between one frame and the next it is also possible to use a DummyTick
//       doing absolutely nothing because the communications between cells through the in and out channels are enough
//       also to keep them in sync.
//       I tried to implement a GlobalTick that doesn't requires a tick per cell, but I failed and I need to review this.

//        this.tick = new GlobalTick(dimensions);
//        this.tick = new DummyTick();
        this.tick = new TickPerCell(dimensions);

        Cell[][] grid = new Cell[dimensions.rows()][dimensions.cols()];

        dimensions.forEachRowCol((r, c) -> grid[r][c] = new Cell(
                r,
                c,
                seed[r][c],
                tick,
                resultChannels.getChannel(r, c),
                new ArrayList<>(),
                new ArrayList<>()));

        dimensions.forEachRowCol((r, c) -> {
            Cell cell = grid[r][c];
            cells.add(cell);
            dimensions.forEachNeighbor(r, c, (ri, ci) -> {
                Cell other = grid[ri][ci];
                Channel<Boolean> ch = new Channel<>();
                cell.addInChannel(ch);
                other.addOutChannel(ch);
            });
        });

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

        Channel<Boolean[][]> gridChannel = new Channel<>(); // channel carries aggregated liveness matrices
        Dimensions dimensions = new Dimensions(pattern.length, pattern[0].length, args.toroidal());
        return GameOfLife.create(args, dimensions, pattern, gridChannel);
    }

    private static GameOfLife create(ExecutionArgs args, Dimensions dimensions, boolean[][] seed, Channel<Boolean[][]> gridChannel) {
        return args.threadPerCell() ?
                new ThreadPerCellGameOfLife(dimensions, seed, args.periodMilliseconds(), gridChannel, args.logRate(), args.useVirtualThreads()) :
                new ThreadPerCoreGameOfLife(dimensions, seed, args.periodMilliseconds(), gridChannel, args.logRate(), args.useVirtualThreads());
    }

    Channel<Boolean[][]> getGridChannel() {
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

    public Boolean[][] calculateFrame() {
        tick.tick();
        resultChannels.forEachChannel( Channel::take, grid ); // populate matrix with results
        gridChannel.put(grid); // emit aggregated liveness matrix
        endOfFrame();
        return grid;
    }

    public Boolean[][] calculateFrameBlocking() {
        runner.accept(() -> calculateFrame());
        return gridChannel.take();
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
}
