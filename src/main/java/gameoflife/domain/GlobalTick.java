package gameoflife.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class GlobalTick implements Tick {

    private final int cellsNr;

    private final Semaphore tickSemaphore = new Semaphore(0);
    private final Semaphore cellsSemaphore = new Semaphore(0);

    private final AtomicInteger awaiting = new AtomicInteger(0);


    public GlobalTick(Dimensions dimensions) {
        this.cellsNr = dimensions.rows() * dimensions.cols();
    }

    @Override
    public synchronized void tick() {
        tickSemaphore.acquireUninterruptibly();
        awaiting.getAndSet(0);
        cellsSemaphore.release(cellsNr);
    }

    @Override
    public synchronized void waitTick(int r, int c) {
        if (awaiting.incrementAndGet() == cellsNr) {
            tickSemaphore.release(1);
        }
        cellsSemaphore.acquireUninterruptibly();
    }
}
