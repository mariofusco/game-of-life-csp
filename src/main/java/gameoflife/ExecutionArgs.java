package gameoflife;

import gameoflife.concurrent.BlockingRendezVous;
import gameoflife.execution.ExecutionStrategy;
import gameoflife.ui.UiRunner;

public record ExecutionArgs(String patternFile, int maxWindowWidth, int maxWindowHeight,
                            int periodMilliseconds,
                            int leftPadding, int topPadding, int rightPadding, int bottomPadding,
                            boolean rotate, boolean toroidal, boolean logRate, ExecutionStrategy executionStrategy, boolean threadPerCell,
                            BlockingRendezVous.Type rendezVousType, UiRunner.Type uiType) {

    public static final boolean IS_NATIVE_IMAGE = System.getProperty("org.graalvm.nativeimage.imagecode") != null;

    private static final ExecutionStrategy DEFAULT_EXECUTION_STRATEGY = ExecutionStrategy.ForkJoinVirtual;

    private static final boolean THREAD_PER_CELL = true; //USE_VIRTUAL_THREADS;

    private static final String DEFAULT_PATTERN = "patterns/gosper_glider_gun.txt";
    private static final int DEFAULT_MAX_WINDOW_WIDTH = 1280;
    private static final int DEFAULT_MAX_WINDOW_HEIGHT = 960;
    private static final int DEFAULT_PERIOD_MILLISECONDS = 0;
    private static final boolean DEFAULT_ROTATE = false;
    private static final boolean DEFAULT_TOROIDAL = false;
    private static final boolean DEFAULT_LOG_RATE = true;
    private static final int DEFAULT_PADDING = 25;

    private static final BlockingRendezVous.Type DEFAULT_RENDEZ_VOUS_TYPE = BlockingRendezVous.Type.OneToOneParking; // BlockingRendezVous.Type.BlockingQueue;

    private static final UiRunner.Type DEFAULT_UI_TYPE = UiRunner.Type.Graphical;

    public static ExecutionArgs parse(String[] args) {
        return new ExecutionArgs(
                args.length > 0 && !args[0].startsWith("ui=") ? args[0] : DEFAULT_PATTERN,
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
                DEFAULT_EXECUTION_STRATEGY,
                args.length > 9 ? Boolean.parseBoolean(args[12]) : THREAD_PER_CELL,
                DEFAULT_RENDEZ_VOUS_TYPE,
                getUiType(args));
    }

    private static UiRunner.Type getUiType(String[] args) {
        for (String uiType : args) {
            if (uiType.startsWith("ui=c") || uiType.startsWith("ui=C")) {
                return UiRunner.Type.Counter;
            }
            if (uiType.startsWith("ui=g") || uiType.startsWith("ui=G")) {
                return UiRunner.Type.Graphical;
            }
            if (uiType.startsWith("ui=t") || uiType.startsWith("ui=T")) {
                return UiRunner.Type.Textual;
            }
        }
        return DEFAULT_UI_TYPE;
    }

    public static ExecutionArgs create(int padding, ExecutionStrategy executionStrategy, boolean threadPerCell, BlockingRendezVous.Type rendezVousType, UiRunner.Type uiType) {
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
                executionStrategy,
                threadPerCell,
                rendezVousType,
                uiType);
    }

    @Override
    public String toString() {
        return "ExecutionArgs{" +
                "patternFile='" + patternFile + '\'' +
                ", periodMilliseconds=" + periodMilliseconds +
                ", rotate=" + rotate +
                ", toroidal=" + toroidal +
                ", logRate=" + logRate +
                ", executionStrategy=" + executionStrategy +
                ", threadPerCell=" + threadPerCell +
                '}';
    }

    @Override
    public boolean logRate() {
        return logRate && uiType() != UiRunner.Type.Textual;
    }

    @Override
    public UiRunner.Type uiType() {
        if (uiType == UiRunner.Type.Graphical && IS_NATIVE_IMAGE) {
            return UiRunner.Type.Textual;
        }
        return uiType;
    }
}