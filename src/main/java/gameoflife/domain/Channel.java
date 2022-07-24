package gameoflife.domain;

import gameoflife.concurrent.BlockingRendezVous;

public class Channel<T> {

    private final BlockingRendezVous<T> rendezVous;

    public Channel(BlockingRendezVous.Type type) {
        rendezVous = BlockingRendezVous.of(type);
    }

    public T take() {
        try {
            return rendezVous.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void put(T value) {
        try {
            rendezVous.put(value);
        } catch (InterruptedException e) {
            // abort
        }
    }
}
