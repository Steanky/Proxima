package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class BasicPathHandler implements PathHandler {
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

        private final Phaser resultPhaser;
        private final Phaser completionPhaser;

        private boolean merged;

        private volatile boolean await;
        private volatile Vec3I contactPoint;
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

            this.resultPhaser = new Phaser();
            this.completionPhaser = new Phaser();
        }

        @Override
        public void run() {
            if (tryMerge()) {
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
                if (!(finished || await) && tryMerge()) {
                    return;
                }
            }
            while (!finished);

            intermediateCompletion.complete(null);

            if (await) {
                resultPhaser.awaitAdvance(0);
            }

            future.complete(pathOperation.makeResult());

            if (await) {
                completionPhaser.awaitAdvance(0);
            }
        }

        private boolean sameDestination(Path other) {
            return destX == other.destX && destY == other.destY && destZ == other.destZ;
        }

        private boolean tryMerge() {
            Path dependentPath = dependent.get();
            if (dependentPath != null) {
                dependent.set(null);

                PathOperation dependentOperation = dependentPath.pathOperation;
                if (dependentOperation == null) {
                    future.completeExceptionally(new IllegalStateException("Expected non-null PathOperation"));

                    //terminate the phasers (we don't know if arrive will succeed)
                    dependentPath.resultPhaser.forceTermination();
                    dependentPath.completionPhaser.forceTermination();
                    return true;
                }

                try {
                    //wait for the path to be found, but not for the operation to create a PathResult yet
                    dependentPath.intermediateCompletion.get();

                    synchronized (dependentOperation.syncTarget()) {
                        synchronized (pathOperation.syncTarget()) {
                            if (!dependentPath.await) {
                                //signals a "perfect merge" where paths are identical
                                //dependent's phasers have NOT been registered!
                                //catch ExecutionException here to avoid calling arrive() on them
                                try {
                                    future.complete(dependentPath.future.get());
                                }
                                catch (InterruptedException | ExecutionException e) {
                                    future.completeExceptionally(e);
                                }

                                return true;
                            }

                            int x = contactPoint.x();
                            int y = contactPoint.y();
                            int z = contactPoint.z();

                            Node dependentNode = dependentOperation.graph().get(x, y, z);

                            //array of node positions pointing back to the origin
                            Vec3I[] vectors = dependentNode.asVectorArray();

                            //let the operation finish
                            dependentPath.resultPhaser.arrive();

                            PathResult result = dependentPath.future.get();
                            Set<Vec3I> resultPath = result.vectors();
                            ObjectSet<Vec3I> ourPath = new ObjectLinkedOpenHashSet<>(resultPath.size());

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
                                        else if (resultVector.equals(contactPoint)) {
                                            append = true;
                                        }
                                    }

                                    break;
                                }
                            }

                            if (!foundMergePoint) {
                                future.completeExceptionally(
                                        new IllegalStateException("No nodes in common with the merged path"));
                                dependentPath.completionPhaser.arrive();
                                return true;
                            }

                            future.complete(new PathResult(ObjectSets.unmodifiable(ourPath), pathOperation
                                    .graph().size(), result.isSuccessful()));
                            dependentPath.completionPhaser.arrive();
                            return true;
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    future.completeExceptionally(e);
                    dependentPath.resultPhaser.arrive();
                    dependentPath.completionPhaser.arrive();
                    return true;
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
    private final ArrayBlockingQueue<Path> operationList;

    public BasicPathHandler(int threads) {
        this.executor = Executors.newFixedThreadPool(threads, runnable ->
                new PathThread(new BasicPathOperation(), runnable));

        //set a capacity such that it's impossible to exceed Phaser's maximum number of registered parties
        this.operationList = new ArrayBlockingQueue<>(65535);

        Thread mergerThread = new Thread(this::merger, "Proxima Path Merger Thread");
        mergerThread.setDaemon(true);
        mergerThread.start();
    }

    private enum MergeResult {
        NEITHER, FIRST, SECOND
    }

    @SuppressWarnings({"LoopConditionNotUpdatedInsideLoop"})
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

                MergeResult result = attemptMerge(operation, previous);
                if (result == MergeResult.FIRST) {
                    iterator.remove();
                }
                else if (result == MergeResult.SECOND) {
                    operationList.poll();
                    break;
                }
            }
        }
    }

    private MergeResult attemptMerge(Path firstPath, Path secondPath) {
        if (!firstPath.settings.equals(secondPath.settings)) { //can't merge if settings are different
            return MergeResult.NEITHER;
        }

        if (firstPath.sameDestination(secondPath)) { //don't consider merge unless the destinations match
            PathOperation first = firstPath.pathOperation;
            PathOperation second = secondPath.pathOperation;

            if (first != null && second != null) {
                synchronized (first.syncTarget()) {
                    synchronized (second.syncTarget()) {
                        if (!first.state().running() || !second.state().running()) {
                            return MergeResult.NEITHER;
                        }

                        if (firstPath.equals(secondPath)) {
                            //exact same position, destination, and settings, so we can easily merge
                            //this will cause a "perfect merge" where no modifications to the path are needed
                            firstPath.merged = true;
                            firstPath.dependent.set(secondPath);
                            return MergeResult.FIRST;
                        }

                        boolean firstMerged = tryPrepareMerge(firstPath, first, secondPath, second);
                        if (firstMerged) {
                            return MergeResult.FIRST;
                        }

                        boolean secondMerged = tryPrepareMerge(secondPath, second, firstPath, first);
                        if (secondMerged) {
                            return MergeResult.SECOND;
                        }

                        return MergeResult.NEITHER;
                    }
                }
            }
        }

        return MergeResult.NEITHER;
    }

    private boolean tryPrepareMerge(Path firstPath, PathOperation first, Path secondPath, PathOperation second) {
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
            secondPath.await = true;

            secondPath.resultPhaser.register();
            secondPath.completionPhaser.register();

            firstPath.merged = true;
            firstPath.dependent.set(secondPath);
            firstPath.contactPoint = Vec3I.immutable(x, y, z);
            return true;
        }

        return false;
    }

    @Override
    public @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings) {
        Path operation = new Path(x, y, z, destX, destY, destZ, settings);

        try {
            operationList.put(operation);
        } catch (InterruptedException e) {
            CompletableFuture<PathResult> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }

        executor.execute(operation);
        return operation.future;
    }
}
