package com.github.steanky.proxima;

import com.github.steanky.vector.HashVec3I2ObjectMap;
import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BasicPathOperation implements PathOperation {
    private final NodeQueue openSet;
    private final Object syncTarget;

    private Vec3I2ObjectMap<Node> graph;

    private Vec3IPredicate successPredicate;
    private Explorer explorer;
    private Heuristic heuristic;

    private State state;

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

            //set the current node, g == 0
            current = new Node(startX, startY, startZ, 0, heuristic.distance(startX, startY, startZ, destinationX,
                    destinationY, destinationZ), Movement.UNKNOWN, null);
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
    public @Nullable PathResult step() {
        if (!openSet.isEmpty()) {
            current = openSet.dequeue();

            //Vec3IPredicate to avoid needing to create a Vec3I object
            //predicate returns true = we found our destination and have a path
            if (successPredicate.test(current.x, current.y, current.z)) {
                //complete (may throw an exception if already completed)
                return complete(current, true);
            }

            //reference the explore method: this is not strictly necessary, but it is cleaner, and prevents from
            //accidentally capturing a variable from step's scope, which would result in unnecessary object allocation
            explorer.exploreEach(current, this::explore);
            if (current.h < best.h) {
                best = current;
            }
        }
        else {
            return complete(best, false);
        }

        return null;
    }

    private void clearDataStructures() {
        openSet.clear();
        openSet.trim(32);

        current = null;
        best = null;
    }

    private PathResult complete(Node best, boolean success) {
        synchronized (syncTarget) {
            if (state == State.COMPLETE) {
                throw new IllegalStateException("Cannot complete already-completed path");
            }

            state = State.COMPLETE;
            PathResult result = new PathResult(best.reverseToVectorList(), graph.size(), success);

            clearDataStructures();
            return result;
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
    public int currentX() {
        return current.x;
    }

    @Override
    public int currentY() {
        return current.y;
    }

    @Override
    public int currentZ() {
        return current.z;
    }

    @Override
    public @NotNull Vec3I2ObjectMap<Node> graph() {
        return graph;
    }

    @Override
    public @NotNull Object syncTarget() {
        return syncTarget;
    }
}