package gameoflife.execution;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import static gameoflife.execution.VirtualThreadUtil.createVirtualThreadFactory;

public enum ExecutionStrategy {
    Native, ForkJoinVirtual, FixedCarrierPoolVirtual, PinnedCarrierVirtual;

    public Consumer<Runnable> getTaskExecutor(int threadPoolSize) {
        return switch (this) {
            case Native -> createFixedThreadPool(threadPoolSize)::execute;
            case ForkJoinVirtual -> Thread::startVirtualThread;
            case FixedCarrierPoolVirtual -> {
                var vThreadFactory = createVirtualThreadFactory(createFixedThreadPool(Runtime.getRuntime().availableProcessors()));
                yield r -> vThreadFactory.newThread(r).start();
            }
            case PinnedCarrierVirtual -> new ThreadPerCoreTaskExecutor(Runtime.getRuntime().availableProcessors());
        };
    }

    private static ExecutorService createFixedThreadPool(int threadPoolSize) {
        return Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    private static class ThreadPerCoreTaskExecutor implements Consumer<Runnable> {

        private final int threadPoolSize;

        private final ThreadFactory[] threadFactories;

        private int tasksCounter = 0;

        private ThreadPerCoreTaskExecutor(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            this.threadFactories = new ThreadFactory[threadPoolSize];
            for (int i = 0; i < threadPoolSize; i++) {
                this.threadFactories[i] = createVirtualThreadFactory(Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                }));
            }
        }

        @Override
        public void accept(Runnable runnable) {
            threadFactories[tasksCounter++ % threadPoolSize].newThread(runnable).start();
        }
    }
}
