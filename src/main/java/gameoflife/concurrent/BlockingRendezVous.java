package gameoflife.concurrent;

public sealed interface BlockingRendezVous<T> permits BlockingQueue, BlockingTransfer, LockedSingleValue, OneToOneParkingSingleValue, OneToOneYieldingSingleValue {

    void put(T x) throws InterruptedException;

    T take() throws InterruptedException;

    enum Type {
        OneToOneYielding,
        OneToOneParking,
        LockedSingleValue,
        BlockingQueue,
        BlockingTransfer
    }


    static <T> BlockingRendezVous<T> of(Type type) {
        return switch (type) {
            case BlockingQueue -> new BlockingQueue<>();
            case BlockingTransfer -> new BlockingTransfer<>();
            case LockedSingleValue -> new LockedSingleValue<>();
            case OneToOneParking -> new OneToOneParkingSingleValue<>();
            case OneToOneYielding -> new OneToOneYieldingSingleValue<>();
            default -> throw new AssertionError("unknown type: " + type);
        };
    }
}
