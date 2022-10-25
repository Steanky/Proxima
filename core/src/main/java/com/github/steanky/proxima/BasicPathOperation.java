package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IBiPredicate;
import org.jetbrains.annotations.NotNull;

public class BasicPathOperation implements PathOperation {
    private final NodeQueue openSet;
    private final Object syncTarget;

    private Vec3I2ObjectMap<Node> graph;

    private Vec3IBiPredicate successPredicate;
    private Explorer explorer;
    private Heuristic heuristic;

    private State state;
    private boolean success;

    private Node current;
    private Node best;

    private int startX;
    private int startY;
    private int startZ;

    private int destinationX;
    private int destinationY;
    private int destinationZ;

    public BasicPathOperation() {
        this.openSet = new NodeQueue();
        this.syncTarget = new Object();
        this.state = State.UNINITIALIZED;
    }

    @Override
    public void init(int startX, int startY, int startZ, int destinationX, int destinationY, int destinationZ,
            @NotNull PathSettings settings) {
        synchronized (syncTarget) {
            clearDataStructures();

            this.graph = settings.graph();

            this.successPredicate = settings.successPredicate();
            this.explorer = settings.explorer();
            this.heuristic = settings.heuristic();

            //indicate that we can start stepping
            state = State.INITIALIZED;
            success = false;

            //set the current node, g == 0
            current = new Node(startX, startY, startZ, 0, heuristic.distance(startX, startY, startZ, destinationX,
                    destinationY, destinationZ), Movement.UNKNOWN, null);
            openSet.enqueue(current);
            best = current;

            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;

            this.destinationX = destinationX;
            this.destinationY = destinationY;
            this.destinationZ = destinationZ;
        }
    }

    @Override
    public boolean step() {
        if (!openSet.isEmpty()) {
            current = openSet.dequeue();

            //Vec3IPredicate to avoid needing to create a Vec3I object
            //predicate returns true = we found our destination and have a path
            if (successPredicate.test(current.x, current.y, current.z, destinationX, destinationY, destinationZ)) {
                //complete (may throw an exception if already completed)
                complete(true);
                return true;
            }

            //reference the explore method: this is not strictly necessary, but it is cleaner, and prevents from
            //accidentally capturing a variable from step's scope, which would result in unnecessary object allocation
            explorer.exploreEach(current, this::explore);
            if (current.h < best.h) {
                best = current;
            }
        }
        else {
            complete(false);
            return true;
        }

        return false;
    }

    private void clearDataStructures() {
        openSet.clear();
        openSet.trim(32);

        current = null;
        best = null;
    }

    private void complete(boolean success) {
        synchronized (syncTarget) {
            if (state == State.COMPLETE) {
                throw new IllegalStateException("Cannot complete already-completed path");
            }

            state = State.COMPLETE;
            this.success = success;
        }
    }

    private void explore(Node current, Movement movementToNeighbor, int x, int y, int z) {
        Node neighbor = graph.computeIfAbsent(x, y, z, this::buildNode);

        float g = current.g + heuristic.distance(current.x, current.y, current.z, neighbor.x, neighbor.y, neighbor.z);
        if (g < neighbor.g) {
            neighbor.parent = current;
            neighbor.g = g;
            openSet.enqueueOrUpdate(neighbor);
        }
    }

    private Node buildNode(int x, int y, int z) {
        return new Node(x, y, z, Float.POSITIVE_INFINITY, heuristic.heuristic(x, y, z, destinationX, destinationY,
                destinationZ), Movement.UNKNOWN, null);
    }

    @Override
    public @NotNull State state() {
        return state;
    }

    @Override
    public int startX() {
        return startX;
    }

    @Override
    public int startY() {
        return startY;
    }

    @Override
    public int startZ() {
        return startZ;
    }

    @Override
    public @NotNull Node current() {
        return current;
    }

    @Override
    public @NotNull Vec3I2ObjectMap<Node> graph() {
        return graph;
    }

    @Override
    public @NotNull PathResult makeResult() {
        if (state.running()) {
            throw new IllegalStateException("Can't compile a result while still running");
        }

        PathResult result = new PathResult(best.reverseToVectorSet(), graph.size(), success);
        clearDataStructures();

        return result;
    }

    @Override
    public @NotNull Object syncTarget() {
        return syncTarget;
    }
}