package gameoflife.concurrent;

import java.util.concurrent.LinkedTransferQueue;

final class BlockingTransfer<T> implements BlockingRendezVous<T> {

    private final LinkedTransferQueue<T> queue = new LinkedTransferQueue<>();

    @Override
    public void put(T x) throws InterruptedException {
        queue.put(x);
    }

    @Override
    public T take() throws InterruptedException {
        return queue.take();
    }
}
