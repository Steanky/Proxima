package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class BasicAsyncPathfinder implements Pathfinder {
    private final ForkJoinPool pathPool;
    private final ThreadLocal<PathOperation> pathOperationLocal;
    private final int poolCapacity;
    private final AtomicInteger poolSize;

    public BasicAsyncPathfinder(@NotNull ForkJoinPool pathPool,
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
                        //exit if interrupted, this can occur if the pathfinder is shut down unexpectedly
                        throw new IllegalStateException("Interrupted during path computation");
                    }
                }

                return localOperation.makeResult();
            }
            finally {
                //decrement the poolSize since this operation is finishing
                poolSize.decrementAndGet();

                //immediately reduce memory pressure by cleaning up the operation; PathOperation instances hang around
                //for a while in ThreadLocals, so we want to make sure they aren't huge
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
                //decrement the poolSize again because the callable wasn't actually added
                poolSize.decrementAndGet();
            }
        }

        try {
            //if the poolCapacity is exceeded, pathfind on the caller thread
            return CompletableFuture.completedFuture(callable.call());
        }
        catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void shutdown() {
        if (pathPool == ForkJoinPool.commonPool()) {
            //common pool can't be shut down
            return;
        }

        pathPool.shutdown();

        if (!pathPool.awaitQuiescence(10, TimeUnit.SECONDS)) {
            //if we took longer than 10 seconds to shut down, interrupt the workers
            pathPool.shutdownNow();

            if (!pathPool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.DAYS)) {
                throw new IllegalStateException("Did you really wait this long?");
            }
        }
    }
}