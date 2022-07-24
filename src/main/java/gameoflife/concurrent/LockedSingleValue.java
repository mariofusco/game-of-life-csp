package gameoflife.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class LockedSingleValue<T> implements BlockingRendezVous<T> {

    private final Lock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private T value;

    @Override
    public void put(T x) throws InterruptedException {
        lock.lock();
        try {
            if (value != null) {
                notFull.await();
            }
            value = x;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T take() throws InterruptedException {
        lock.lock();
        try {
            if (value == null) {
                notEmpty.await();
            }
            T x = value;
            value = null;
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }
}
