package gameoflife.domain;

import gameoflife.concurrent.BlockingSingleValue;

import static gameoflife.domain.ChannelsGrid.makeGrid;

public class TickPerCell implements Tick {

    private final ChannelsGrid<Boolean> tickChannels;

    public TickPerCell(Dimensions dimensions, BlockingSingleValue.Type type) {
        this.tickChannels = makeGrid(dimensions, type);
    }

    @Override
    public void tick() {
        tickChannels.forEachChannel( c -> c.put(true) ); // emit tick event for every cell
    }

    @Override
    public void waitTick(int r, int c) {
        tickChannels.getChannel(r, c).take();
    }
}
