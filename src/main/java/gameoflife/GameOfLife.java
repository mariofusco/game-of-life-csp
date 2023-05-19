package gameoflife;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import gameoflife.concurrent.BlockingRendezVous;
import gameoflife.domain.Cell;
import gameoflife.domain.Channel;
import gameoflife.domain.ChannelsGrid;
import gameoflife.domain.Dimensions;
import gameoflife.domain.Tick;
import gameoflife.domain.TickPerCell;
import gameoflife.execution.ExecutionStrategy;
import gameoflife.ui.PatternParser;

import static gameoflife.domain.ChannelsGrid.makeGrid;

public abstract class GameOfLife {

    protected final List<Cell> cells = new ArrayList<>();

    private final Dimensions dimensions;
    private final int period;
    private final boolean logRate;
    private final Channel<boolean[][]> gridChannel;

    private final Tick tick;
    private final ChannelsGrid<Boolean> resultChannels;

    private long lastStatsDump = System.nanoTime();
    private long framesCount = 0;

    protected final Consumer<Runnable> runner;

    public GameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel,
                      boolean logRate, ExecutionStrategy executionStrategy, BlockingRendezVous.Type channelType) {
        this.dimensions = dimensions;
        this.gridChannel = gridChannel;
        this.period = period;
        this.logRate = logRate;
        this.resultChannels = makeGrid(dimensions, channelType);

// TODO: The tick mechanism is now pluggable but for now I'm keeping the original one sending one tick per cell.
//       In reality when no delay is required between one frame and the next it is also possible to use a DummyTick
//       doing absolutely nothing because the communications between cells through the in and out channels are enough
//       also to keep them in sync.
//       I tried to implement a GlobalTick that doesn't requires a tick per cell, but I failed and I need to review this.

//        this.tick = new GlobalTick(dimensions);
//        this.tick = new DummyTick();
        this.tick = new TickPerCell(dimensions, channelType);

        Cell[][] grid = new Cell[dimensions.rows()][dimensions.cols()];

        dimensions.forEachRowCol( (r, c) ->
            cells.add(grid[r][c] = new Cell(r, c, seed[r][c], tick, resultChannels.getChannel(r, c)))
        );

        dimensions.forEachRowCol( (r, c) ->
            dimensions.forEachNeighbor(r, c, (ri, ci) -> {
                Channel<Boolean> ch = new Channel<>(channelType);
                grid[r][c].addInChannel(ch);
                grid[ri][ci].addOutChannel(ch);
            })
        );

        this.runner = executionStrategy.getTaskExecutor(getThreadPoolSize());
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

        Channel<boolean[][]> gridChannel = new Channel<>(args.rendezVousType()); // channel carries aggregated liveness matrices
        Dimensions dimensions = new Dimensions(pattern.length, pattern[0].length, args.toroidal());
        return GameOfLife.create(args, dimensions, pattern, gridChannel);
    }

    private static GameOfLife create(ExecutionArgs args, Dimensions dimensions, boolean[][] seed, Channel<boolean[][]> gridChannel) {
        return args.threadPerCell() ?
                new ThreadPerCellGameOfLife(dimensions, seed, args.periodMilliseconds(), gridChannel, args.logRate(), args.executionStrategy(), args.rendezVousType()) :
                new ThreadPerCoreGameOfLife(dimensions, seed, args.periodMilliseconds(), gridChannel, args.logRate(), args.executionStrategy(), args.rendezVousType());
    }

    public Channel<boolean[][]> getGridChannel() {
        return gridChannel;
    }

    public Dimensions getDimensions() {
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
        tick.tick();
        boolean[][] grid = new boolean[dimensions.rows()][dimensions.cols()];
        resultChannels.forEachChannel( Channel::take, grid ); // populate matrix with results
        gridChannel.put(grid); // emit aggregated liveness matrix
        endOfFrame();
        return grid;
    }

    public boolean[][] calculateFrameBlocking() {
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
