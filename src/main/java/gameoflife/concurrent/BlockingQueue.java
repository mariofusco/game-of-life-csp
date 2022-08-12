package gameoflife.concurrent;

import java.util.concurrent.LinkedBlockingQueue;

final class BlockingQueue<T> implements BlockingRendezVous<T> {

    private final LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>(1);

    @Override
    public void put(T x) throws InterruptedException {
        queue.put(x);
    }

    @Override
    public T take() throws InterruptedException {
        return queue.take();
    }
}
