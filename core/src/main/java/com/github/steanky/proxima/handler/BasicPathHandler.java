package com.github.steanky.proxima.handler;

import com.github.steanky.proxima.BasicPathOperation;
import com.github.steanky.proxima.PathOperation;
import com.github.steanky.proxima.PathResult;
import com.github.steanky.proxima.PathSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class BasicPathHandler implements PathHandler {
    private static final class PathfindThread extends Thread {
        private final PathOperation operation;

        private PathfindThread(PathOperation operation, Runnable runnable) {
            super(runnable);
            this.operation = operation;
        }
    }

    private static final class Path implements Runnable {
        private static final int MERGE_CHECK_INTERVAL = 16;

        private final int x;
        private final int y;
        private final int z;

        private final int destX;
        private final int destY;
        private final int destZ;

        private final PathSettings settings;

        private boolean merged;
        private final AtomicReference<Path> dependent;
        private final CompletableFuture<PathResult> future;

        private volatile PathOperation pathOperation;

        private Path(int x, int y, int z, int destX, int destY, int destZ, @NotNull PathSettings settings) {
            this.x = x;
            this.y = y;
            this.z = z;

            this.destX = destX;
            this.destY = destY;
            this.destZ = destZ;

            this.settings = settings;

            this.dependent = new AtomicReference<>();
            this.future = new CompletableFuture<>();
        }

        @Override
        public void run() {
            if (mergeIfPossible()) {
                //even though we haven't initialized a path yet, it's possible we can merge right away
                //this can happen if we have the same settings, position, and destination as another path
                return;
            }

            PathfindThread thread = (PathfindThread)Thread.currentThread();

            //one PathOperation per thread
            pathOperation = thread.operation;
            pathOperation.init(x, y, z, destX, destY, destZ, settings);

            PathResult result;
            int i = 0;
            do {
                result = pathOperation.step();

                //not much point to checking for merges every step
                if (i++ == MERGE_CHECK_INTERVAL) {
                    if (mergeIfPossible()) {
                        return;
                    }

                    i = 0;
                }
            }
            while (result == null);

            this.future.complete(result);
        }

        private boolean sameDestination(Path other) {
            return destX == other.destX && destY == other.destY && destZ == other.destZ;
        }

        private boolean mergeIfPossible() {
            Path result = dependent.get();
            if (result != null) {
                try {
                    this.future.complete(result.future.get());
                } catch (InterruptedException | ExecutionException e) {
                    this.future.completeExceptionally(e);
                }

                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, destX, destY, destZ, settings);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj == this) {
                return true;
            }

            if (obj instanceof Path operation) {
                return operation.x == x && operation.y == y && operation.z == z && operation.destX == destX &&
                        operation.destY == destY && operation.destZ == destZ && operation.settings.equals(settings);
            }

            return false;
        }
    }

    private final Executor executor;
    private final BlockingQueue<Path> operationList;

    public BasicPathHandler(int threads) {
        this.executor = Executors.newFixedThreadPool(threads - 1, runnable ->
                new PathfindThread(new BasicPathOperation(), runnable));

        this.operationList = new LinkedBlockingQueue<>();

        Thread mergerThread = new Thread(this::merger, "Proxima Path Merger Thread");
        mergerThread.setDaemon(true);
        mergerThread.start();
    }

    private void merger() {
        while (true) {
            Thread.onSpinWait();

            while (operationList.size() > 1) {
                Iterator<Path> iterator = operationList.iterator();

                Path previous = null;
                while (iterator.hasNext()) {
                    Path operation = iterator.next();
                    if (operation.merged) {
                        iterator.remove();
                        continue;
                    }

                    if (previous == null) {
                        previous = operation;
                        continue;
                    }

                    if (canMerge(operation, previous)) {
                        operation.merged = true;
                        operation.dependent.set(previous);
                        iterator.remove();
                    }
                }
            }
        }
    }

    private boolean canMerge(Path dependent, Path dependee) {
        if (!dependent.settings.equals(dependee.settings)) { //can't merge if settings are different
            return false;
        }

        if (dependent.equals(dependee)) {
            //exact same position, destination, and settings, so we can easily merge
            //there's no need to alter anything in regard to the path
            //if a lot of paths from the same spot are queued, this can happen frequently
            return true;
        }

        if (dependent.sameDestination(dependee)) { //don't consider merge unless the destinations match
            PathOperation first = dependent.pathOperation;
            PathOperation second = dependee.pathOperation;

            if (first != null && second != null) {
                synchronized (first.syncTarget()) {
                    synchronized (second.syncTarget()) {
                        if (!first.state().running()) {
                            //must be in a running state for merge to be useful
                            return false;
                        }


                    }
                }
            }
        }

        return false;
    }

    @Override
    public @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings) {
        Path operation = new Path(x, y, z, destX, destY, destZ, settings);

        operationList.add(operation);
        executor.execute(operation);

        return operation.future;
    }
}
