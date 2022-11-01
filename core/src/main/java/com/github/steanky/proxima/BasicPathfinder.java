package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class BasicPathfinder implements Pathfinder {
    private final ForkJoinPool pathPool;
    private final ThreadLocal<PathOperation> pathOperationLocal;
    private final int poolCapacity;
    private final AtomicInteger poolSize;

    public BasicPathfinder(@NotNull ForkJoinPool pathPool,
            @NotNull Supplier<? extends PathOperation> pathOperationSupplier, int poolCapacity) {
        this.pathPool = Objects.requireNonNull(pathPool);
        this.pathOperationLocal = ThreadLocal.withInitial(pathOperationSupplier);
        if (poolCapacity <= 0) {
            throw new IllegalArgumentException("executorCapacity cannot be negative");
        }
        this.poolCapacity = poolCapacity;
        this.poolSize = new AtomicInteger();
    }

    @Override
    public @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings) {
        Callable<PathResult> callable = () -> {
            PathOperation localOperation = pathOperationLocal.get();

            try {
                localOperation.init(x, y, z, destX, destY, destZ, settings);

                //step the path until the method reports completion by returning false
                while (!localOperation.step()) {
                    if (Thread.interrupted()) {
                        throw new IllegalStateException("Interrupted during path computation");
                    }
                }

                return localOperation.makeResult();
            }
            finally {
                //immediately reduce memory pressure by cleaning up the operation
                localOperation.cleanup();
            }
        };

        if (poolSize.get() < poolCapacity) {
            try {
                poolSize.incrementAndGet();
                return pathPool.submit(callable);
            }
            catch (RejectedExecutionException ignored) {
                //if execution is rejected, run the callable on the caller thread
            }
            finally {
                //make sure we always decrement the poolSize after
                poolSize.decrementAndGet();
            }
        }

        try {
            return CompletableFuture.completedFuture(callable.call());
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void shutdown() {
        pathPool.shutdown();

        try {
            if (!pathPool.awaitTermination(10, TimeUnit.SECONDS)) {
                pathPool.shutdownNow();

                if (!pathPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                    throw new IllegalStateException("Did you really wait this long?");
                }
            }
        }
        catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted when waiting for shutdown");
        }
    }
}