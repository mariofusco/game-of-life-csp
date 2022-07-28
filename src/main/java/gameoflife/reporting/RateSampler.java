package gameoflife.reporting;

import java.util.function.LongSupplier;

public final class RateSampler implements Runnable {

    private final LongSupplier sampling;
    private long lastSample;
    private long lastSampleTime;
    private long rate;
    private long timeSpanNs;

    private RateSampler(final LongSupplier sampling) {
        this.sampling = sampling;
        this.timeSpanNs = 0;
        this.lastSampleTime = System.nanoTime();
        this.lastSample = sampling.getAsLong();
        this.rate = 0;
    }

    @Override
    public void run() {
        final long now = System.nanoTime();
        final long newSample = sampling.getAsLong();
        rate = newSample - lastSample;
        timeSpanNs = now - lastSampleTime;
        lastSample = newSample;
        lastSampleTime = now;
    }

    public long getLastSample() {
        return lastSample;
    }

    public long reportRate(final long reportIntervalNs) {
        return (rate * reportIntervalNs) / timeSpanNs;
    }

    public static RateSampler of(final LongSupplier sampling) {
        return new RateSampler(sampling);
    }
}
