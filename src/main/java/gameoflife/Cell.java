package gameoflife;

import java.util.List;

public class Cell {
    private boolean alive;

    private final Channel<Boolean> tickChannel;
    private final Channel<Boolean> resultChannel;
    private final List<Channel<Boolean>> inChannels;
    private final List<Channel<Boolean>> outChannels;

    Cell(CellOptions options) {
        this.alive = options.alive();
        this.tickChannel = options.tickChannel();
        this.resultChannel = options.resultChannel();
        this.inChannels = options.inChannels();
        this.outChannels = options.outChannels();
    }

    void run() {
        while (true) {
            evaluateCell();
        }
    }

    void evaluateCell() {
        notifyLiveness();
        calculateNextState();
    }

    void notifyLiveness() {
        tickChannel.take(); // wait for tick stimulus
        outChannels.forEach(ch -> ch.put(alive)); // announce liveness to neighbors
    }

    void calculateNextState() {
        int neighbors = inChannels.stream().map(Channel::take).mapToInt(b -> b ? 1 : 0).sum(); // receive liveness from neighbors
        alive = nextState(alive, neighbors); // calculate next state based on game of life rules
        resultChannel.put(alive); // announce resulting next state
    }

    private static boolean nextState(boolean alive, int neighbors) {
        return neighbors == 3 || (alive && neighbors == 2);
    }
}
