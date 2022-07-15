package gameoflife.domain;

import java.util.function.BiConsumer;

public record Dimensions(int rows, int cols, boolean toroidal) {
    public void forEachRowCol(BiConsumer<Integer, Integer> consumer) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                consumer.accept(r, c);
            }
        }
    }

    public void forEachNeighbor(int row, int col, BiConsumer<Integer, Integer> consumer) {
        for (int r = row - 1; r <= row + 1; r++) { // [row-1, row+1]
            for (int c = col - 1; c <= col + 1; c++) { // [col-1, col+1]
                if (toroidal) {
                    int r1 = r < 0 ? rows-1 : (r == rows ? 0 : r);
                    int c1 = c < 0 ? cols-1 : (c == cols ? 0 : c);
                    registerConsumer(row, col, consumer, r1, c1);
                } else {
                    if (r >= 0 && r < rows && c >= 0 && c < cols) { // exclude out of bounds
                        registerConsumer(row, col, consumer, r, c);
                    }
                }
            }
        }
    }

    private void registerConsumer(int row, int col, BiConsumer<Integer, Integer> consumer, int r, int c) {
        if (r != row || c != col) { // exclude self
            consumer.accept(r, c);
        }
    }

    @Override
    public String toString() {
        return "Dimensions{" +
                "rows=" + rows +
                ", cols=" + cols +
                ", toroidal=" + toroidal +
                '}';
    }
}
