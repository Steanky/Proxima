package com.github.steanky.proxima.path;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class BasicAsyncPathfinder implements Pathfinder {
    private final ExecutorService pathExecutor;
    private final ThreadLocal<PathOperation> pathOperationLocal;
    private final int poolCapacity;
    private final AtomicInteger poolSize;

    public BasicAsyncPathfinder(@NotNull ExecutorService pathExecutor,
            @NotNull Supplier<? extends PathOperation> pathOperationSupplier, int poolCapacity) {
        this.pathExecutor = Objects.requireNonNull(pathExecutor);
        this.pathOperationLocal = ThreadLocal.withInitial(pathOperationSupplier);
        if (poolCapacity <= 0) {
            throw new IllegalArgumentException("executorCapacity must be positive");
        }
        this.poolCapacity = poolCapacity;
        this.poolSize = new AtomicInteger();
    }

    @Override
    public @NotNull Future<PathResult> pathfind(double x, double y, double z, @NotNull PathTarget destination,
            @NotNull PathSettings settings) {
        Callable<PathResult> callable = () -> {
            PathOperation localOperation = null;
            try {
                //resolving a destination might be expensive, so do it on the pathfinder thread
                Vec3I destinationVector = destination.resolve();

                if (destinationVector == null) {
                    //invalid destination; return immediately
                    return PathResult.EMPTY;
                }

                localOperation = pathOperationLocal.get();
                localOperation.init(x, y, z, destinationVector.x(), destinationVector.y(), destinationVector.z(),
                        settings);

                //step the path until the method reports completion by returning false
                while (!localOperation.step()) {
                    if (Thread.interrupted()) {
                        //exit if interrupted, this can occur if the pathfinder is shut down unexpectedly
                        return PathResult.EMPTY;
                    }
                }

                return localOperation.makeResult();
            }
            finally {
                //decrement the poolSize since this operation is finishing
                poolSize.decrementAndGet();

                if (localOperation != null) {
                    //immediately reduce memory pressure by cleaning up the operation; PathOperation instances hang around
                    //for a while in ThreadLocals, so we want to make sure they aren't huge
                    localOperation.cleanup();
                }
            }
        };

        if (poolSize.get() < poolCapacity) {
            try {
                poolSize.incrementAndGet();
                return pathExecutor.submit(callable);
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
        if (pathExecutor == ForkJoinPool.commonPool()) {
            //common pool can't be shut down
            //don't await quiescence either: there may be tasks unrelated to pathfinding being performed which we don't
            //care about waiting for
            return;
        }

        pathExecutor.shutdown();

        try {
            if (!pathExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                //if we took longer than 10 seconds to shut down, interrupt the workers
                pathExecutor.shutdownNow();
            }
        }
        catch (InterruptedException ignored) {
            //if interrupted, just return immediately
        }
    }
}