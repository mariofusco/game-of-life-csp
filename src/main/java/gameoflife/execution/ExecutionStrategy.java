package gameoflife.execution;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static gameoflife.execution.VirtualThreadUtil.createVirtualThreadFactory;

public enum ExecutionStrategy {
    Native, ForkJoinVirtual, FixedCarrierPoolVirtual;

    public Consumer<Runnable> getTaskExecutor(int threadPoolSize) {
        return switch (this) {
            case Native -> createFixedThreadPool(threadPoolSize)::execute;
            case ForkJoinVirtual -> Thread::startVirtualThread;
            case FixedCarrierPoolVirtual -> {
                var vThreadFactory = createVirtualThreadFactory(createFixedThreadPool(threadPoolSize));
                yield r -> vThreadFactory.newThread(r).start();
            }
        };
    }

    private static ExecutorService createFixedThreadPool(int threadPoolSize) {
        return Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }
}
