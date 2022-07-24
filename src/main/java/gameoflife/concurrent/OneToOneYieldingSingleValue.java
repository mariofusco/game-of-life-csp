package gameoflife.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

final class OneToOneYieldingSingleValue<T> extends AtomicReference<T> implements BlockingSingleValue<T> {
    private static final int INTERRUPT_CHECKS = 100;

    @Override
    public void put(T x) throws InterruptedException {
        Objects.requireNonNull(x);
        int noChecks = 0;
        while (get() != null) {
            Thread.yield();
            noChecks++;
            if (noChecks == INTERRUPT_CHECKS) {
                noChecks = 0;
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        }
        lazySet(x);
    }

    @Override
    public T take() throws InterruptedException {
        T t;
        int noChecks = 0;
        while ((t = get()) == null) {
            Thread.yield();
            noChecks++;
            if (noChecks == INTERRUPT_CHECKS) {
                noChecks = 0;
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        }
        lazySet(null);
        return t;
    }
}
