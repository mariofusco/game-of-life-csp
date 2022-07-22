package gameoflife.domain;

import gameoflife.concurrent.BlockingSingleValue;

public class Channel<T> {

    private final BlockingSingleValue<T> singleValue;

    public Channel(BlockingSingleValue.Type type) {
        singleValue = BlockingSingleValue.of(type);
    }

    public T take() {
        try {
            return singleValue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void put(T value) {
        try {
            singleValue.put(value);
        } catch (InterruptedException e) {
            // abort
        }
    }
}
