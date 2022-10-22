package com.github.steanky.proxima.handler;

import com.github.steanky.proxima.BasicPathOperation;
import com.github.steanky.proxima.PathOperation;
import com.github.steanky.proxima.PathResult;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
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

    private static final class Operation implements Runnable {
        private static final int MERGE_INTERVAL = 16;

        private final int x;
        private final int y;
        private final int z;

        private final int destX;
        private final int destY;
        private final int destZ;

        private boolean merged;
        private final AtomicReference<Operation> merge;
        private final CompletableFuture<PathResult> future;

        private volatile PathOperation pathOperation;

        private Operation(int x, int y, int z, int destX, int destY, int destZ) {
            this.x = x;
            this.y = y;
            this.z = z;

            this.destX = destX;
            this.destY = destY;
            this.destZ = destZ;

            this.merge = new AtomicReference<>();
            this.future = new CompletableFuture<>();
        }

        @Override
        public void run() {
            if (mergeIfPossible()) {
                return;
            }

            //likely faster than ThreadLocal but has the same effect
            PathfindThread thread = (PathfindThread)Thread.currentThread();
            pathOperation = thread.operation;
            pathOperation.init(x, y, z, destX, destY, destZ, null, null, null,
                    null, null);

            PathResult result;
            int i = 0;
            do {
                result = pathOperation.step();

                if (i++ == MERGE_INTERVAL) {
                    if (mergeIfPossible()) {
                        return;
                    }

                    i = 0;
                }
            }
            while (result == null);

            this.future.complete(result);
        }

        private boolean mergeIfPossible() {
            Operation result = merge.get();
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
    }

    private final ThreadPoolExecutor executor;
    private final BlockingQueue<Operation> operationList;

    public BasicPathHandler(int threads) {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads - 1, runnable -> {
            PathOperation operation = new BasicPathOperation();
            return new PathfindThread(operation, runnable);
        });

        this.operationList = new LinkedBlockingQueue<>();

        Thread mergerThread = new Thread(this::merger, "Proxima Path Merger Thread");
        mergerThread.setDaemon(true);
        mergerThread.start();
    }

    private void merger() {
        while (true) {
            Thread.onSpinWait();

            while (operationList.size() > 1) {
                Iterator<Operation> iterator = operationList.iterator();

                Operation previous = null;
                while (iterator.hasNext()) {
                    Operation operation = iterator.next();
                    if (operation.merged) {
                        iterator.remove();
                        continue;
                    }

                    if (previous == null) {
                        previous = operation;
                        continue;
                    }

                    if (canMerge(previous, operation)) {
                        operation.merged = true;
                        operation.merge.set(previous);
                        iterator.remove();
                    }
                }
            }
        }
    }

    private boolean canMerge(Operation first, Operation second) {
        PathOperation firstOperation = first.pathOperation;
        PathOperation secondOperation = second.pathOperation;

        if (firstOperation != null && secondOperation != null) {
            //synchronize to prevent certain state changes in the target operations
            //these state changes (path completion and initialization) shouldn't happen too frequently
            synchronized (firstOperation.syncTarget()) {
                synchronized (secondOperation.syncTarget()) {
                    if (firstOperation.state() != PathOperation.State.UNINITIALIZED &&
                            secondOperation.state() != PathOperation.State.UNINITIALIZED) {

                    }
                }
            }
        }

        return false;
    }

    @Override
    public @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ) {
        Operation operation = new Operation(x, y, z, destX, destY, destZ);
        operationList.add(operation);
        executor.execute(operation);
        return operation.future;
    }
}
