package gameoflife.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JPanel;

import gameoflife.ExecutionArgs;
import gameoflife.GameOfLife;
import gameoflife.domain.Dimensions;

public class WindowOutput implements Consumer<boolean[][]> {

    private final GameOfLife gameOfLife;
    private final int width;
    private final int height;
    private final Canvas canvas;
    private volatile boolean[][] cells;

    WindowOutput(ExecutionArgs args, GameOfLife gameOfLife) {
        this.gameOfLife = gameOfLife;
        Dimensions dimensions = gameOfLife.getDimensions();
        double scale = calculateScale(dimensions.rows(), dimensions.cols(), args.maxWindowWidth(), args.maxWindowHeight());
        this.width = (int) (scale * dimensions.cols());
        this.height = (int) (scale * dimensions.rows());
        canvas = new Canvas();
        JFrame frame = new JFrame("Conway's Game of Life");
        frame.add(canvas);
        frame.setSize(width, height);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void accept(boolean[][] cells) {
        this.cells = cells;
        this.canvas.repaint();
    }

    class Canvas extends JPanel {
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (cells == null)
                return;
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.LIGHT_GRAY);
            int cellWidth = width / cells[0].length;
            for (int x = 0; x <= width; x += cellWidth) {
                g.drawLine(x, 0, x, height);
            }
            int cellHeight = height / cells.length;
            for (int y = 0; y <= height; y += cellHeight) {
                g.drawLine(0, y, width, y);
            }
            g.setColor(Color.BLACK);
            for (int r = 0; r < cells.length; r++) {
                for (int c = 0; c < cells[r].length; c++) {
                    if (cells[r][c]) {
                        g.fillRect(c * cellWidth, r * cellHeight, cellWidth, cellHeight);
                    }
                }
            }
        }
    }

    private static double calculateScale(int rows, int cols, int maxWindowWidth, int maxWindowHeight) {
        double aspect = (double) maxWindowWidth / maxWindowHeight;
        double actual = (double) cols / rows;
        return actual < aspect
                ? (double) maxWindowHeight / rows
                : (double) maxWindowWidth / cols;
    }
}
