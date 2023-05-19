package gameoflife.execution;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class VirtualThreadUtil {

    private static final MethodHandle currentCarrierThread;
    private static final MethodHandle virtualThreadFactory;

    static {
        MethodHandle ct;
        MethodHandle vtf;
        try {
            MethodHandles.Lookup thr = MethodHandles.privateLookupIn(Thread.class, MethodHandles.lookup());
            ct = thr.findStatic(Thread.class, "currentCarrierThread", MethodType.methodType(Thread.class));
            Class<?> vtbClass = Class.forName("java.lang.ThreadBuilders$VirtualThreadBuilder");
            vtf = thr.findConstructor(vtbClass, MethodType.methodType(void.class, Executor.class));
            // create efficient transformer
            vtf = vtf.asType(MethodType.methodType(Thread.Builder.OfVirtual.class, Executor.class));
        } catch (Throwable e) {
            System.err.println("The following JMV options are required: --enable-preview --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED");
            throw new RuntimeException(e);
        }
        currentCarrierThread = ct;
        virtualThreadFactory = vtf;
    }

    public static boolean isVirtual(Thread thread) {
        return thread != null && thread.isVirtual();
    }

    public static Thread currentCarrierThread() {
        try {
            Thread currentThread = Thread.currentThread();
            return currentThread.isVirtual() ? (Thread) currentCarrierThread.invokeExact() : currentThread;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public static ThreadFactory createVirtualThreadFactory(Executor executor) {
        return createVirtualThreadFactory(executor, null);
    }

    public static ThreadFactory createVirtualThreadFactory(Executor executor, String name) {
        var ov = createVirtualThreadBuilder(executor);
        if (name != null) {
            ov.name(name);
        }
        return ov.factory();
    }

    public static Thread.Builder.OfVirtual createVirtualThreadBuilder(Executor executor) {
        try {
            return (Thread.Builder.OfVirtual) virtualThreadFactory.invoke(executor);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
