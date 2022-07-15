package gameoflife.domain;

public interface Tick {

    void tick();

    void waitTick(int r, int c);
}
