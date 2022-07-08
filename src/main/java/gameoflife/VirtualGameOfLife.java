package gameoflife;

public class VirtualGameOfLife extends GameOfLife {
    VirtualGameOfLife(Dimensions dimensions, boolean[][] seed, int period, Channel<boolean[][]> gridChannel, boolean logRate) {
        super(dimensions, seed, period, gridChannel, logRate);
    }

    @Override
    public void startCells() {
        cells.forEach(Cell::start);
    }

    @Override
    public void startGame() {
        Thread.startVirtualThread(this::run);
    }
}
