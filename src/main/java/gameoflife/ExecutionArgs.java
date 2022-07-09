package gameoflife;

public record ExecutionArgs(String patternFile, int maxWindowWidth, int maxWindowHeight,
                     int periodMilliseconds,
                     int leftPadding, int topPadding, int rightPadding, int bottomPadding,
                     boolean rotate, boolean toroidal, boolean logRate, boolean useVirtualThreads, boolean threadPerCell) {

    private static final boolean USE_VIRTUAL_THREADS = true;
    private static final boolean THREAD_PER_CELL = USE_VIRTUAL_THREADS;

    private static final String DEFAULT_PATTERN = "patterns/gosper_glider_gun.txt";
    private static final int DEFAULT_MAX_WINDOW_WIDTH = 1280;
    private static final int DEFAULT_MAX_WINDOW_HEIGHT = 960;
    private static final int DEFAULT_PERIOD_MILLISECONDS = 0;
    private static final boolean DEFAULT_ROTATE = false;
    private static final boolean DEFAULT_TOROIDAL = false;
    private static final boolean DEFAULT_LOG_RATE = true;
    private static final int DEFAULT_PADDING = 25;

    public static ExecutionArgs parse(String[] args) {
        return new ExecutionArgs(
                args.length > 0 ? args[0] : DEFAULT_PATTERN,
                args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_MAX_WINDOW_WIDTH,
                args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_MAX_WINDOW_HEIGHT,
                args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_PERIOD_MILLISECONDS,
                args.length > 4 ? Integer.parseInt(args[4]) : DEFAULT_PADDING,
                args.length > 5 ? Integer.parseInt(args[5]) : DEFAULT_PADDING,
                args.length > 6 ? Integer.parseInt(args[6]) : DEFAULT_PADDING,
                args.length > 7 ? Integer.parseInt(args[7]) : DEFAULT_PADDING,
                args.length > 8 ? Boolean.parseBoolean(args[8]) : DEFAULT_ROTATE,
                args.length > 8 ? Boolean.parseBoolean(args[9]) : DEFAULT_TOROIDAL,
                args.length > 9 ? Boolean.parseBoolean(args[10]) : DEFAULT_LOG_RATE,
                args.length > 9 ? Boolean.parseBoolean(args[11]) : USE_VIRTUAL_THREADS,
                args.length > 9 ? Boolean.parseBoolean(args[12]) : THREAD_PER_CELL);
    }

    public static ExecutionArgs create(boolean useVirtualThreads, boolean threadPerCell) {
        return create(DEFAULT_PADDING, useVirtualThreads, threadPerCell);
    }

    public static ExecutionArgs create(int padding, boolean useVirtualThreads, boolean threadPerCell) {
        return new ExecutionArgs(
                DEFAULT_PATTERN,
                DEFAULT_MAX_WINDOW_WIDTH,
                DEFAULT_MAX_WINDOW_HEIGHT,
                DEFAULT_PERIOD_MILLISECONDS,
                padding,
                padding,
                padding,
                padding,
                DEFAULT_ROTATE,
                DEFAULT_TOROIDAL,
                DEFAULT_LOG_RATE,
                useVirtualThreads,
                threadPerCell);
    }
}