package gameoflife.reporting;

import gameoflife.GameOfLife;

import java.io.PrintStream;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class FrameCount {

    public static Thread reportWith(ThreadFactory threadFactory, GameOfLife game, PrintStream out) {
        return threadFactory.newThread(() -> {
            final Thread current = Thread.currentThread();
            final long intervalInNs = TimeUnit.SECONDS.toNanos(1);
            final RateSampler sampler = RateSampler.of(game::frameCount);
            try {
                while (!current.isInterrupted()) {
                    TimeUnit.NANOSECONDS.sleep(intervalInNs);
                    sampler.run();
                    final long rate = sampler.reportRate(intervalInNs);
                    out.printf("%d fps\n", rate);
                }
            } catch (InterruptedException ignore) {

            }
        });
    }
}
