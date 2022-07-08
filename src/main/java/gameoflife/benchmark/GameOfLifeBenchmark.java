package gameoflife.benchmark;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import gameoflife.ExecutionArgs;
import gameoflife.GameOfLife;
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
@Warmup(iterations = 1)
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(2)
public class GameOfLifeBenchmark {

    @Param({"true", "false"})
    private boolean useVirtualThreads;

    @Param({"5", "25", "100"}) // 874, 5074, 49324
    private int padding;

    private ExecutorService executor;

    private GameOfLife gameOfLife;

    @Setup
    public void setup() {
        executor = useVirtualThreads ?
                Executors.newVirtualThreadPerTaskExecutor() :
                Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                });
        ExecutionArgs args = ExecutionArgs.create(padding, useVirtualThreads);
        gameOfLife = GameOfLife.create(args);
        gameOfLife.startCells();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public boolean[][] benchmark() {
        try {
            return executor.submit( () -> gameOfLife.calculateFrame() ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
