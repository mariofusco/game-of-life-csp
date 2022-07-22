package gameoflife.concurrent;

public sealed interface BlockingSingleValue<T> permits LockedSingleValue, OneToOneParkingSingleValue, OneToOneYieldingSingleValue {

    void put(T x) throws InterruptedException;

    T take() throws InterruptedException;

    enum Type {
        OneToOneYielding,
        OneToOneParking,
        MultiLocked
    }


    static <T> BlockingSingleValue<T> of(Type type) {
        return switch (type) {
            case MultiLocked -> new LockedSingleValue<>();
            case OneToOneParking -> new OneToOneParkingSingleValue<>();
            case OneToOneYielding -> new OneToOneYieldingSingleValue<>();
            default -> throw new AssertionError("unknown type: " + type);
        };
    }
}
