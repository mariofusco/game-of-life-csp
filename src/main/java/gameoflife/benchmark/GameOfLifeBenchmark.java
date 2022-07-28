package gameoflife.benchmark;

import java.util.concurrent.TimeUnit;

import gameoflife.ExecutionArgs;
import gameoflife.GameOfLife;
import gameoflife.concurrent.BlockingRendezVous;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(2)
public class GameOfLifeBenchmark {

    @Param({"true", "false"})
    private boolean useVirtualThreads;

    @Param({"true", "false"})
    private boolean threadPerCell;

    @Param({"5", "25", "100"}) // 874, 5074, 49324 cells
    private int padding;

    @Param({"BlockingQueue", "BlockingTransfer", "LockedSingleValue", "OneToOneParking"})
    private String channelType;

    // see https://github.com/openjdk/loom/blob/0ec74059f13c00e79e579fbafda0ad855170ea76/src/java.base/share/classes/java/lang/VirtualThread.java#L1024
    @Param({"0", "2", "4"})
    private int parallelism;

    private GameOfLife gameOfLife;

    @Setup
    public void setup() {
        if (!useVirtualThreads && threadPerCell && parallelism != 0) {
            throw new IllegalStateException("parallelism has no effects for this benchmark");
        }
        if (!useVirtualThreads && threadPerCell && padding > 50) {
            // This condition causes a OOM, skip it
            return;
        }
        if (parallelism > 0) {
            System.setProperty("jdk.virtualThreadScheduler.parallelism", Integer.toString(parallelism));
        }
        ExecutionArgs args = ExecutionArgs.create(padding, useVirtualThreads, threadPerCell,
                BlockingRendezVous.Type.valueOf(channelType), false);
        gameOfLife = GameOfLife.create(args);
        gameOfLife.startCells();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public boolean[][] benchmark() {
        final GameOfLife gameOfLife = this.gameOfLife;
        return gameOfLife != null ? gameOfLife.calculateFrameBlocking() : null;
    }
}
