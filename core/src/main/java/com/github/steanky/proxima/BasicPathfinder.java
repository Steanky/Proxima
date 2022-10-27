package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class BasicPathfinder implements Pathfinder {
    private static final class Path implements Runnable {
        private final int x;
        private final int y;
        private final int z;

        private final int destX;
        private final int destY;
        private final int destZ;

        private final PathSettings settings;
        private final ThreadLocal<PathOperation> operationLocal;

        private final AtomicReference<Path> dependent;
        private final CompletableFuture<Void> intermediateCompletion;
        private final CompletableFuture<PathResult> future;

        private final Phaser resultPhaser;
        private final Phaser completionPhaser;

        private boolean merged;

        private volatile boolean await;
        private volatile Vec3I contactPoint;
        private volatile PathOperation pathOperation;

        private Path(int x, int y, int z, int destX, int destY, int destZ,
                @NotNull PathSettings settings, @NotNull ThreadLocal<PathOperation> operationLocal) {
            this.x = x;
            this.y = y;
            this.z = z;

            this.destX = destX;
            this.destY = destY;
            this.destZ = destZ;

            this.settings = settings;
            this.operationLocal = operationLocal;
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

            //one PathOperation per thread
            pathOperation = operationLocal.get();
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

            pathOperation.cleanup();
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
                    dependentPath.terminatePhasers();
                    return true;
                }

                try {
                    //wait for the path to be found, but not for the operation to create a PathResult yet
                    dependentPath.intermediateCompletion.get();

                    synchronized (dependentOperation.syncTarget()) {
                        synchronized (pathOperation.syncTarget()) {
                            if (dependentPath.await) {
                                int x = contactPoint.x();
                                int y = contactPoint.y();
                                int z = contactPoint.z();

                                Vec3I2ObjectMap<Node> dependentOperationGraph = dependentOperation.graph();
                                Node dependentContactNode = dependentOperationGraph.get(x, y, z);

                                Vec3I[] vectors = dependentContactNode.asVectorArray();

                                dependentPath.resultPhaser.arrive();

                                PathResult dependentResult = dependentPath.future.get();

                                Set<Vec3I> dependentSolution = dependentResult.vectors();

                                Vec3I mergePoint = null;
                                int midLegSize = 0;
                                for (Vec3I vector : vectors)  {
                                    if (dependentSolution.contains(vector)) {
                                        mergePoint = vector;
                                        break;
                                    }

                                    midLegSize++;
                                }

                                if (mergePoint == null) {
                                    dependentPath.terminatePhasers();
                                    future.completeExceptionally(new IllegalStateException("Failed to find common " +
                                            "merge point"));
                                    return true;
                                }

                                Node dependentMergeNode = dependentOperationGraph.get(mergePoint.x(), mergePoint.y(),
                                        mergePoint.z());

                                dependentPath.completionPhaser.arrive();

                                int finalLegSize = dependentMergeNode.size();

                                Vec3I2ObjectMap<Node> ourGraph = pathOperation.graph();
                                Node ourMergeNode = ourGraph.get(x, y, z);

                                int firstLegSize = ourMergeNode.size() - 1;

                                Vec3I[] setVectors = new Vec3I[firstLegSize + midLegSize + finalLegSize];
                                int j = 0;

                                Node parent = ourMergeNode.parent;
                                if (parent != null) {
                                    j = parent.reversedAddAll(setVectors);
                                }

                                for (int i = 0; i < midLegSize; i++) {
                                    setVectors[j + i] = dependentContactNode.vector();
                                    dependentContactNode = dependentContactNode.parent;
                                }

                                for (int i = 0; i < finalLegSize; i++) {
                                    setVectors[j + i] = dependentMergeNode.vector();
                                    dependentMergeNode = dependentMergeNode.parent;
                                }

                                this.future.complete(new PathResult(Set.of(setVectors), ourGraph.size(),
                                        true));
                                return true;
                            }
                        }
                    }

                    //signals a "perfect merge" where paths are identical
                    //dependent's phasers have NOT been registered, but terminate them anyway
                    //catch ExecutionException here to avoid calling arrive() on them
                    //only happens if dependentPath.await is false
                    try {
                        future.complete(dependentPath.future.get());
                    }
                    catch (InterruptedException | ExecutionException e) {
                        dependentPath.terminatePhasers();
                        future.completeExceptionally(e);
                    }

                    return true;
                } catch (InterruptedException | ExecutionException e) {
                    dependentPath.terminatePhasers();
                    future.completeExceptionally(e);
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

        private void terminatePhasers() {
            resultPhaser.forceTermination();
            completionPhaser.forceTermination();
        }
    }

    private final ExecutorService executor;
    private final ThreadLocal<List<Path>> pathListLocal;
    private final ThreadLocal<PathOperation> pathOperationLocal;

    public BasicPathfinder(@NotNull ExecutorService pathExecutor,
            @NotNull Supplier<? extends PathOperation> operationSupplier) {
        this.executor = Objects.requireNonNull(pathExecutor);
        this.pathListLocal = ThreadLocal.withInitial(LinkedList::new);
        this.pathOperationLocal = ThreadLocal.withInitial(Objects.requireNonNull(operationSupplier));
    }

    private enum MergeResult {
        NEITHER, FIRST, SECOND
    }

    private void doMerges(List<Path> pathQueue) {
        if (pathQueue.size() <= 1) {
            return;
        }

        Iterator<Path> iterator = pathQueue.iterator();

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
                pathQueue.remove(0);
                break;
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
        Node current = first.current();

        int x = current.x;
        int y = current.y;
        int z = current.z;

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
        Path operation = new Path(x, y, z, destX, destY, destZ, settings, pathOperationLocal);

        List<Path> pathList = pathListLocal.get();
        pathList.add(operation);
        if (pathList.size() > 1) {
            doMerges(pathList);
        }

        executor.execute(operation);
        return operation.future;
    }

    @Override
    public void shutdown() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Executor did not terminate within the time window");
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while waiting for PathHandler shutdown");
        }
    }
}