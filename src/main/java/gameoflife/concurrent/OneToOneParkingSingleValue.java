package gameoflife.concurrent;

import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

final class OneToOneParkingSingleValue<T> implements BlockingSingleValue<T> {
    private volatile Thread waitSomeValue = null;
    private volatile Thread waitEmpty = null;
    private volatile T value;

    @Override
    public void put(T x) throws InterruptedException {
        Objects.requireNonNull(x);
        while (value != null) {
            final Thread currentThread = Thread.currentThread();
            waitEmpty = currentThread;
            try {
                if (value == null) {
                    break;
                }
                LockSupport.park();
                if (currentThread.isInterrupted()) {
                    throw new InterruptedException();
                }
            } finally {
                waitEmpty = null;
            }
        }
        value = x;
        LockSupport.unpark(waitSomeValue);
    }

    @Override
    public T take() throws InterruptedException {
        T t;
        while ((t = value) == null) {
            final Thread currentThread = Thread.currentThread();
            waitSomeValue = currentThread;
            try {
                t = value;
                if (t != null) {
                    break;
                }
                LockSupport.park();
                if (currentThread.isInterrupted()) {
                    throw new InterruptedException();
                }
            } finally {
                waitSomeValue = null;
            }
        }
        assert t != null;
        value = null;
        LockSupport.unpark(waitEmpty);
        return t;
    }
}
