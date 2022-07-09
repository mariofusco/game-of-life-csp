package gameoflife.domain;

import java.util.List;

public class CellsGroup {
    private final List<Cell> cells;

    public CellsGroup(List<Cell> cells) {
        this.cells = cells;
    }

    public void run() {
        while (true) {
            cells.forEach( Cell::notifyLiveness );
            cells.forEach( Cell::calculateNextState );
        }
    }
}
