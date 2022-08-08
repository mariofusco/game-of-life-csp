package gameoflife.domain;

import java.util.ArrayList;
import java.util.List;

public class Cell {
    private final int row;
    private final int col;
    private final Tick tick;
    private final Channel<Boolean> resultChannel;
    private final List<Channel<Boolean>> inChannels = new ArrayList<>();
    private final List<Channel<Boolean>> outChannels = new ArrayList<>();

    private boolean alive;

    public Cell(int row, int col, boolean alive, Tick tick, Channel<Boolean> resultChannel) {
        this.row = row;
        this.col = col;
        this.tick = tick;
        this.resultChannel = resultChannel;
        this.alive = alive;
    }

    public void run() {
        while (true) {
            notifyLiveness();
            calculateNextState();
        }
    }

    void notifyLiveness() {
        tick.waitTick(row, col); // wait for tick stimulus
        outChannels.forEach(ch -> ch.put(alive)); // announce liveness to neighbors
    }

    void calculateNextState() {
        int neighbors = inChannels.stream().map(Channel::take).mapToInt(b -> b ? 1 : 0).sum(); // receive liveness from neighbors
        alive = neighbors == 3 || (alive && neighbors == 2); // calculate next state based on game of life rules
        resultChannel.put(alive); // announce resulting next state
    }

    public void addInChannel(Channel<Boolean> channel) {
        inChannels.add(channel);
    }

    public void addOutChannel(Channel<Boolean> channel) {
        outChannels.add(channel);
    }
}
