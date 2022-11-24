package gameoflife.ui;

import java.util.function.Consumer;

public class CountingOutput implements Consumer<boolean[][]> {
    static final int TIMED_frames = 5000;
    static final int WARMUP_frames = 2000;

    private int frames = 0;
    private long timingStartNanos;

    @Override
    public void accept(boolean[][] cells) {
        if (frames == 0) {
            System.out.println("\nStarting warmup\n");
        }
        frames++;
        if ((frames & 0xf) == 0) {
            System.out.print(".");
        }
        if (frames == WARMUP_frames) {
            System.out.println("\n\nStarting benchmark\n");
            timingStartNanos = System.nanoTime();
        } else if (frames == WARMUP_frames + TIMED_frames) {
            long millis = (System.nanoTime() - timingStartNanos) / 1_000_000;
            System.out.printf("%nCompleted %d frames in %dms = %f frames per second%n", TIMED_frames, millis, TIMED_frames / (millis / 1000.0));
            System.exit(0);
        }
    }
}