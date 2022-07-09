package gameoflife.domain;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Channel<T> {
    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>(); // LinkedTransferQueue

    public T take() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void put(T value) {
        try {
            queue.put(value);
        } catch (InterruptedException e) {
            // abort
        }
    }
}
