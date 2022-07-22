package gameoflife.domain;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

public class Channel<T> {
//    private final BlockingQueue<T> queue = new ArrayBlockingQueue<>(1);
//    private final BlockingQueue<T> queue = new LinkedTransferQueue<>();
    private final BlockingSingleValue<T> queue = new BlockingSingleValue<>();

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
