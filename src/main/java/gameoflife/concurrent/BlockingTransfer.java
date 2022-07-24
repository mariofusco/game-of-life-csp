package gameoflife.concurrent;

import java.util.concurrent.LinkedBlockingQueue;

final class BlockingTransfer<T> implements BlockingRendezVous<T> {

    private final BlockingTransfer<T> queue = new BlockingTransfer<>();

    @Override
    public void put(T x) throws InterruptedException {
        queue.put(x);
    }

    @Override
    public T take() throws InterruptedException {
        return queue.take();
    }
}
