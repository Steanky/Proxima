package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class BasicPathHandler implements PathHandler {
    private static final int SLEEP_DURATION_MS = 100;
    private static final class PathThread extends Thread {
        private final PathOperation operation;

        private PathThread(PathOperation operation, Runnable runnable) {
            super(runnable);
            this.operation = operation;
        }
    }

    private static final class Path implements Runnable {
        private final int x;
        private final int y;
        private final int z;

        private final int destX;
        private final int destY;
        private final int destZ;

        private final PathSettings settings;
        private final AtomicReference<Path> dependent;
        private final CompletableFuture<Void> intermediateCompletion;
        private final CompletableFuture<PathResult> future;

        private boolean merged;

        private volatile Vec3I mergePoint;
        private volatile boolean delayResult;
        private volatile boolean delayTermination;
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
            this.intermediateCompletion = new CompletableFuture<>();
            this.future = new CompletableFuture<>();
        }

        @Override
        public void run() {
            if (tryExactMerge()) {
                //even though we haven't initialized a path yet, it's possible we can merge right away
                //this can happen if we have the same settings, position, and destination as another path
                return;
            }

            PathThread thread = (PathThread)Thread.currentThread();

            //one PathOperation per thread
            pathOperation = thread.operation;
            pathOperation.init(x, y, z, destX, destY, destZ, settings);

            boolean finished;
            do {
                finished = pathOperation.step();
                if (!finished && tryMergeWithAdjust()) {
                    return;
                }
            }
            while (!finished);

            intermediateCompletion.complete(null);
            while (delayResult) {
                //wait if we have a thread that needs to merge with us
                Thread.onSpinWait();
            }

            this.future.complete(pathOperation.makeResult());

            while (delayTermination) {
                //wait to terminate until the merging thread finishes its operation
                //otherwise, the same thread could start running a different path in meanwhile
                Thread.onSpinWait();
            }
        }

        private boolean sameDestination(Path other) {
            return destX == other.destX && destY == other.destY && destZ == other.destZ;
        }

        private boolean tryExactMerge() {
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

        private boolean tryMergeWithAdjust() {
            Path dependentPath = dependent.get();
            if (dependentPath != null) {
                dependent.set(null);

                try {
                    PathOperation dependentOperation = dependentPath.pathOperation;
                    if (dependentOperation == null) {
                        future.completeExceptionally(new IllegalStateException("Expected non-null PathOperation"));
                        return true;
                    }

                    //wait for the path to be found, but not for the operation to create a PathResult yet
                    dependentPath.intermediateCompletion.get();

                    synchronized (dependentOperation.syncTarget()) {
                        synchronized (pathOperation.syncTarget()) {
                            int x = mergePoint.x();
                            int y = mergePoint.y();
                            int z = mergePoint.z();

                            Node dependentNode = dependentOperation.graph().get(x, y, z);
                            Vec3I[] vectors = dependentNode.asVectorArray();

                            //allow the path to continue
                            dependentPath.delayResult = false;
                            PathResult result = dependentPath.future.get();
                            Set<Vec3I> resultPath = result.vectors();
                            Set<Vec3I> ourPath = new ObjectLinkedOpenHashSet<>(resultPath.size());

                            boolean foundMergePoint = false;
                            for (Vec3I vec : vectors) {
                                ourPath.add(vec);

                                if (resultPath.contains(vec)) {
                                    foundMergePoint = true;

                                    boolean append = false;
                                    for (Vec3I resultVector : resultPath) {
                                        if (append) {
                                            ourPath.add(resultVector);
                                        }
                                        else if (resultVector.equals(mergePoint)) {
                                            append = true;
                                        }
                                    }

                                    break;
                                }
                            }

                            if (!foundMergePoint) {
                                future.completeExceptionally(
                                        new IllegalStateException("No nodes in common with the merged path"));
                                return true;
                            }

                            future.complete(new PathResult(ourPath, pathOperation.graph().size(),
                                    result.isSuccessful()));
                            return true;
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    future.completeExceptionally(e);
                    return true;
                }
                finally {
                    //make sure to reset these flags to avoid freezing the thread
                    dependentPath.delayResult = false;
                    dependentPath.delayTermination = false;
                }
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
                new PathThread(new BasicPathOperation(), runnable));

        this.operationList = new LinkedBlockingQueue<>();

        Thread mergerThread = new Thread(this::merger, "Proxima Path Merger Thread");
        mergerThread.setDaemon(true);
        mergerThread.start();
    }

    @SuppressWarnings({"LoopConditionNotUpdatedInsideLoop", "BusyWait"})
    private void merger() {
        while (true) {
            //wait until at least 2 paths are in the queue
            while (operationList.size() <= 1) {
                Thread.onSpinWait();
            }

            Iterator<Path> iterator = operationList.iterator();

            Path previous = null;
            while (iterator.hasNext()) {
                Path operation = iterator.next();
                if (operation.merged || operation.future.isDone()) {
                    iterator.remove();
                    continue;
                }

                if (previous == null) {
                    previous = operation;
                    continue;
                }

                if (canMerge(operation, previous)) {
                    iterator.remove();
                }
            }

            try {
                //give some time for the paths to progress
                Thread.sleep(SLEEP_DURATION_MS);
            } catch (InterruptedException ignored) {

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

                        int x = first.currentX();
                        int y = first.currentY();
                        int z = first.currentZ();

                        Node node = second.graph().get(x, y, z);
                        if (node == null) {
                            //no merge if we didn't run into any of the nodes covered by the other graph
                            return false;
                        }

                        Node last = node;
                        while(node != null) {
                            //unidirectional means we can't pass going the opposite direction
                            if (node.movement == Movement.UNIDIRECTIONAL) {
                                return false;
                            }

                            node = node.parent;
                            if (node != null) {
                                last = node;
                            }
                        }

                        //TODO: other ways to filter out undesirable merges to make sure paths don't look weird

                        if (last.x == second.startX() && last.y == second.startY() && last.z == second.startZ()) {
                            //avoid race condition by setting this inside the state lock
                            dependee.delayResult = true;
                            dependee.delayTermination = true;

                            dependent.merged = true;
                            dependent.dependent.set(dependee);
                            dependent.mergePoint = Vec3I.immutable(x, y, z);
                            return true;
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
