package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class BasicPathfinder implements Pathfinder {
    private final class Path implements Runnable {
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

        private boolean started;
        private boolean merged;

        private volatile boolean await;
        private volatile Node dependentContactNode;
        private volatile PathOperation pathOperation;

        private Path(int x, int y, int z, int destX, int destY, int destZ, PathSettings settings) {
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
            try {
                if (tryMerge()) {
                    //even though we haven't initialized a path yet, it's possible we can merge right away
                    //this can happen if we have the same settings, position, and destination as another path
                    //we won't need a PathOperation for this
                    return;
                }

                //one PathOperation per thread
                pathOperation = pathOperationLocal.get();
                pathOperation.init(x, y, z, destX, destY, destZ, settings);

                boolean finished;
                do {
                    if (Thread.interrupted()) {
                        future.completeExceptionally(new InterruptedException("Path computation interrupted"));
                        return;
                    }

                    finished = pathOperation.step();
                    if (!(finished || await) && tryMerge()) {
                        //if tryMerge returns true, it completed our future
                        return;
                    }
                }
                while (!finished);

                intermediateCompletion.complete(null);

                //await will be true if there are other paths depending on this one
                if (await) {
                    resultPhaser.awaitAdvance(0);
                }

                future.complete(pathOperation.makeResult());

                if (await) {
                    completionPhaser.awaitAdvance(0);
                }
            }
            finally {
                if (pathOperation != null) {
                    pathOperation.cleanup();
                }

                pathOperation = null;
                executorSize.decrementAndGet();
            }
        }

        private boolean sameDestination(Path other) {
            return destX == other.destX && destY == other.destY && destZ == other.destZ;
        }

        private boolean sameOrigin(Path other) {
            return x == other.x && y == other.y && z == other.z;
        }


        private boolean tryMerge() {
            Path dependentPath = dependent.get();
            if (dependentPath != null) {
                dependent.set(null);

                PathOperation dependentOperation = dependentPath.pathOperation;
                if (dependentOperation == null || pathOperation == null) {
                    try {
                        future.complete(dependentPath.future.get());
                        return true;
                    } catch (ExecutionException | InterruptedException e) {
                        future.completeExceptionally(e);
                    }

                    return false;
                }

                try {
                    //wait for the path to be found, but not for the operation to create a PathResult yet
                    dependentPath.intermediateCompletion.get();

                    synchronized (dependentOperation.stateSync()) {
                        synchronized (pathOperation.stateSync()) {
                            if (!dependentPath.await) {
                                future.completeExceptionally(new IllegalStateException("Dependent path has not been " +
                                        "prepared for merge"));
                                return false;
                            }

                            Vec3I2ObjectMap<Node> dependentOperationGraph = dependentOperation.graph();
                            Vec3I[] vectors = dependentContactNode.asVectorArray();

                            if (dependentPath.resultPhaser.arrive() < 0) {
                                return false;
                            }

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
                                return false;
                            }


                            Node dependentMergeNode;
                            synchronized (dependentOperation.graphSync()) {
                                dependentMergeNode = dependentOperationGraph.get(mergePoint.x(), mergePoint.y(),
                                        mergePoint.z());
                            }

                            if (dependentPath.completionPhaser.arrive() < 0) {
                                return false;
                            }

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
                } catch (InterruptedException | ExecutionException e) {
                    future.completeExceptionally(e);
                    return true;
                }
            }

            return false;
        }
    }

    private final ExecutorService executor;
    private final Queue<Path> pathQueue;
    private final AtomicInteger pathQueueSize;
    private final AtomicInteger executorSize;
    private final int pathQueueCapacity;
    private final int executorCapacity;
    private final ThreadLocal<PathOperation> pathOperationLocal;

    public BasicPathfinder(@NotNull ExecutorService pathExecutor,
            @NotNull Supplier<? extends PathOperation> operationSupplier, int pathQueueCapacity, int executorCapacity) {
        this.executor = Objects.requireNonNull(pathExecutor);
        this.pathQueue = new ConcurrentLinkedQueue<>();
        this.pathQueueSize = new AtomicInteger();
        this.executorSize = new AtomicInteger();
        this.pathQueueCapacity = pathQueueCapacity;
        this.executorCapacity = executorCapacity;
        this.pathOperationLocal = ThreadLocal.withInitial(Objects.requireNonNull(operationSupplier));

        Thread mergeThread = new Thread(this::doMerges, "Proxima Path Merger Daemon");
        mergeThread.setDaemon(true);
        mergeThread.start();
    }

    private enum MergeResult {
        NEITHER, FIRST, SECOND
    }

    @SuppressWarnings("LoopConditionNotUpdatedInsideLoop")
    private void doMerges() {
        while (true) {
            while (pathQueue.peek() == null) {
                Thread.onSpinWait();
            }

            Iterator<Path> iterator = pathQueue.iterator();
            Path previous = null;
            while (iterator.hasNext()) {
                Path operation = iterator.next();
                if (operation.merged || operation.future.isDone()) {
                    iterator.remove();
                    pathQueueSize.decrementAndGet();
                    continue;
                }

                if (previous == null) {
                    previous = operation;
                    if (!operation.started) {
                        operation.started = true;
                        execute(operation);
                    }

                    continue;
                }

                MergeResult result = attemptMerge(operation, previous);
                if (!operation.started) {
                    operation.started = true;

                    //submitting after merging potentially allows us to instantly complete, without needing to
                    //re-initialize the pathoperation
                    execute(operation);
                }

                if (result == MergeResult.FIRST) {
                    iterator.remove();
                    pathQueueSize.decrementAndGet();
                }
                else if (result == MergeResult.SECOND) {
                    Path path = pathQueue.poll();
                    if (path != null) {
                        pathQueueSize.decrementAndGet();

                        if (path != previous && !path.started) {
                            path.started = true;
                            execute(path);
                        }
                    }

                    if (!previous.started) {
                        previous.started = true;
                        execute(previous);
                    }
                }
            }
        }
    }

    private MergeResult attemptMerge(Path firstPath, Path secondPath) {
        if (!firstPath.settings.equals(secondPath.settings)) { //can't merge if settings are different
            return MergeResult.NEITHER;
        }

        if (firstPath.sameDestination(secondPath)) {
            if (firstPath.sameOrigin(secondPath)) {
                //exact same position, destination, and settings, so we can easily merge
                //this will cause a "perfect merge" where no modifications to the path are needed
                if (firstPath.started && !secondPath.started) {
                    secondPath.merged = true;
                    secondPath.dependent.set(firstPath);
                    return MergeResult.SECOND;
                }
                else if(firstPath.started || secondPath.started) {
                    firstPath.merged = true;
                    firstPath.dependent.set(secondPath);
                    return MergeResult.FIRST;
                }

                return MergeResult.NEITHER;
            }

            PathOperation first = firstPath.pathOperation;
            PathOperation second = secondPath.pathOperation;

            if (first == null || second == null) {
                return MergeResult.NEITHER;
            }

            synchronized (first.stateSync()) {
                synchronized (second.stateSync()) {
                    if (!first.state().running() || !second.state().running()) {
                        return MergeResult.NEITHER;
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

        return MergeResult.NEITHER;
    }

    private boolean tryPrepareMerge(Path firstPath, PathOperation first, Path secondPath, PathOperation second) {
        Node current = first.current();

        int x = current.x;
        int y = current.y;
        int z = current.z;

        Node node;
        synchronized (second.graphSync()) {
            node = second.graph().get(x, y, z);
        }

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
            firstPath.dependentContactNode = last;
            return true;
        }

        return false;
    }

    private void execute(Path path) {
        if (executorSize.get() < executorCapacity) {
            try {
                executor.submit(path);
                executorSize.incrementAndGet();
            }
            catch (RejectedExecutionException e) {
                //executorSize is decremented slightly before the task actually finishes, meaning we (rarely) get this
                //exception
                path.run();
            }
        }
        else {
            //if using an ExecutorService with no bounded capacity, run the task on whatever thread we are in
            path.run();
        }
    }

    @Override
    public @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings) {
        Path operation = new Path(x, y, z, destX, destY, destZ, settings);
        if (pathQueueSize.get() < pathQueueCapacity) {
            //add this operation to the queue, it will be considered for merging
            pathQueueSize.incrementAndGet();
            pathQueue.offer(operation);
        }
        else {
            //if our path queue has grown too big, execute directly, this path won't be considered for merging though
            execute(operation);
        }

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