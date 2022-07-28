package gameoflife;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Consumer;

import gameoflife.concurrent.BlockingRendezVous;
import gameoflife.domain.Cell;
import gameoflife.domain.Channel;
import gameoflife.domain.ChannelsGrid;
import gameoflife.domain.Dimensions;
import gameoflife.domain.Tick;
import gameoflife.domain.TickPerCell;
import gameoflife.ui.PatternParser;

import static gameoflife.domain.ChannelsGrid.makeGrid;

public abstract class GameOfLife {

    private static final AtomicLongFieldUpdater<GameOfLife> FRAME_COUNT =
            AtomicLongFieldUpdater.newUpdater(GameOfLife.class, "framesCount");
    protected final List<Cell> cells = new ArrayList<>();

    private final Dimensions dimensions;
    private final int period;
    private final Channel<boolean[][]> gridChannel;

    private final Channel<boolean[][]> frameCompleted;
    private final boolean[][] grid;
    private final Tick tick;
    private final ChannelsGrid<Boolean> resultChannels;
    private volatile long framesCount = 0;
    protected final Consumer<Runnable> runner;

    public GameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel,
                      boolean useVirtualThreads, BlockingRendezVous.Type channelType) {
        this.dimensions = dimensions;
        this.gridChannel = gridChannel;
        this.period = period;
        this.resultChannels = makeGrid(dimensions, channelType);
        // no UI is involved
        if (gridChannel == null) {
            this.grid = new boolean[dimensions.rows()][dimensions.cols()];
            this.frameCompleted = new Channel<>(BlockingRendezVous.Type.OneToOneParking);
        } else {
            this.grid = null;
            this.frameCompleted = null;
        }
// TODO: The tick mechanism is now pluggable but for now I'm keeping the original one sending one tick per cell.
//       In reality when no delay is required between one frame and the next it is also possible to use a DummyTick
//       doing absolutely nothing because the communications between cells through the in and out channels are enough
//       also to keep them in sync.
//       I tried to implement a GlobalTick that doesn't requires a tick per cell, but I failed and I need to review this.

//        this.tick = new GlobalTick(dimensions);
//        this.tick = new DummyTick();
        this.tick = new TickPerCell(dimensions, channelType);

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
                Channel<Boolean> ch = new Channel<>(channelType);
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
        // channel carries aggregated liveness matrices
        Channel<boolean[][]> gridChannel = args.emitGrid() ? new Channel<>(BlockingRendezVous.Type.BlockingTransfer) : null;
        Dimensions dimensions = new Dimensions(pattern.length, pattern[0].length, args.toroidal());
        return GameOfLife.create(args, dimensions, pattern, gridChannel);
    }

    private static GameOfLife create(ExecutionArgs args, Dimensions dimensions, boolean[][] seed, Channel<boolean[][]> gridChannel) {
        return args.threadPerCell() ?
                new ThreadPerCellGameOfLife(dimensions, seed, args.periodMilliseconds(), gridChannel, args.useVirtualThreads(), args.type()) :
                new ThreadPerCoreGameOfLife(dimensions, seed, args.periodMilliseconds(), gridChannel, args.useVirtualThreads(), args.type());
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
        if (gridChannel == null) {
            throw new IllegalStateException("startGame is supported only with an emitting gridChannel");
        }
        assert grid == null;
        runner.accept(this::run);
    }

    private void run() {
        final int rows = dimensions.rows();
        final int cols = dimensions.cols();
        while (true) {
            calculateFrame(gridChannel, new boolean[rows][cols]);
        }
    }

    public boolean[][] calculateFrame(Channel<boolean[][]> result, boolean[][] grid) {
        tick.tick();
        resultChannels.forEachChannel(Channel::take, grid); // populate matrix with results
        result.put(grid);
        endOfFrame();
        return grid;
    }

    public boolean[][] calculateFrameBlocking() {
        if (gridChannel != null) {
            throw new IllegalStateException("calculateFrameBlocking is supported only without a gridChannel");
        }
        assert grid != null;
        runner.accept(() -> calculateFrame(frameCompleted, grid));
        final boolean[][] result = frameCompleted.take();
        assert result == grid;
        return result;
    }

    private void endOfFrame() {
        if (period > 0) {
            try {
                Thread.sleep(period);
            } catch (InterruptedException ignore) {
            }
        }
        // lightweight metric
        FRAME_COUNT.lazySet(this, framesCount + 1);
    }

    public long frameCount() {
        return framesCount;
    }
}
