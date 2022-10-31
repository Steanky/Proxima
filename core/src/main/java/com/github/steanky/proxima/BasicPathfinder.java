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
    private class PathTask extends ForkJoinTask<PathResult> {
        private final int x;
        private final int y;
        private final int z;

        private final int destX;
        private final int destY;
        private final int destZ;

        private final PathSettings settings;

        private volatile boolean mergeFlag;
        private volatile boolean completeFlag;
        private volatile boolean awaitFlag;

        private volatile PathTask mergeTarget;

        private final Phaser resultPhaser;
        private final Phaser exitPhaser;

        private PathResult result;

        private PathTask(int x, int y, int z, int destX, int destY, int destZ, PathSettings settings) {
            this.x = x;
            this.y = y;
            this.z = z;

            this.destX = destX;
            this.destY = destY;
            this.destZ = destZ;

            this.settings = settings;

            this.resultPhaser = new Phaser();
            this.exitPhaser = new Phaser();
        }

        @Override
        public PathResult getRawResult() {
            return result;
        }

        @Override
        protected void setRawResult(PathResult value) {
            this.result = value;
        }

        @Override
        protected boolean exec() {
            PathOperation localOperation = pathOperationLocal.get();

            try {
                poolSize.incrementAndGet();
                localOperation.init(x, y, z, destX, destY, destZ, settings);

                synchronized (this) {
                    try {
                        //step the path until the method reports completion by returning false
                        while (!localOperation.step()) {
                            if (Thread.interrupted()) {
                                throw new IllegalStateException("Interrupted during path computation");
                            }

                            PathResult mergedResult = merge();
                            if (mergedResult != null) {
                                result = mergedResult;
                                return true;
                            }
                        }
                    }
                    finally {
                        //make sure the completion flag is always set just before exiting the monitor
                        completeFlag = true;
                    }
                }

                boolean await = awaitFlag;
                if (await) {
                    resultPhaser.awaitAdvance(0);
                }

                this.result = localOperation.makeResult();

                if (await) {
                    exitPhaser.awaitAdvance(0);
                }

                return true;
            }
            finally {
                //immediately reduce memory pressure by cleaning up the operation
                localOperation.cleanup();
                poolSize.decrementAndGet();
            }
        }

        private PathResult merge() {
            if (mergeFlag) {
                PathTask mergeTarget = this.mergeTarget;
                if (mergeTarget == null) {
                    throw new IllegalArgumentException("mergeFlag was set, but mergeTarget was null");
                }


            }

            return null;
        }
    }

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

    private ForkJoinTask<PathResult> acquireTask(int x, int y, int z, int destX, int destY, int destZ,
            PathSettings settings) {
        return new PathTask(x, y, z, destX, destY, destZ, settings);
    }

    @Override
    public @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings) {
        ForkJoinTask<PathResult> task = acquireTask(x, y, z, destX, destY, destZ, settings);
        if (poolSize.get() < poolCapacity) {
            try {
                return pathPool.submit(task);
            }
            catch (RejectedExecutionException ignored) {}
        }

        //if the pool is full, or rejected the task, run it on the current thread
        return CompletableFuture.completedFuture(task.invoke());
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