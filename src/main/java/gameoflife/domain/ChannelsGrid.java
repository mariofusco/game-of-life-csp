package gameoflife.domain;

import gameoflife.concurrent.BlockingRendezVous;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class ChannelsGrid<T> {

    private final Channel<T>[][] grid;

    private final Dimensions dimensions;

    public ChannelsGrid(Channel<T>[][] grid, Dimensions dimensions) {
        this.grid = grid;
        this.dimensions = dimensions;
    }

    public Channel<T> getChannel(int r, int c) {
        return grid[r][c];
    }

    public void forEachChannel(Consumer<Channel<T>> consumer) {
        dimensions.forEachRowCol( (r, c) -> consumer.accept( getChannel(r, c) ) );
    }

    public void forEachChannel(Predicate<Channel<T>> f, boolean[][] result) {
        dimensions.forEachRowCol( (r, c) -> result[r][c] = f.test( getChannel(r, c) ) );
    }

    public static <T> ChannelsGrid<T> makeGrid(Dimensions dimensions, BlockingRendezVous.Type type) {
        return new ChannelsGrid<>( IntStream.range(0, dimensions.rows())
                .mapToObj(r -> IntStream.range(0, dimensions.cols()).mapToObj(c -> new Channel<T>(type)).toArray(Channel[]::new))
                .toArray(Channel[][]::new), dimensions );
    }
}
