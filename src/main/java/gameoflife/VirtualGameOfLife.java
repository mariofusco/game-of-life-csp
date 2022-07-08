package gameoflife;

public class VirtualGameOfLife extends GameOfLife {
    VirtualGameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel, boolean logRate) {
        super(dimensions, seed, period, gridChannel, logRate);
    }

    @Override
    public void start() {
        cells.forEach(Cell::start);
        Thread.startVirtualThread(this::run);
    }
}
