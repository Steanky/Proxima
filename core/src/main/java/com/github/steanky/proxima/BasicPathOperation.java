package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IBiPredicate;
import org.jetbrains.annotations.NotNull;

public class BasicPathOperation implements PathOperation {
    private final NodeQueue openSet;

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
        this.state = State.UNINITIALIZED;
    }

    @Override
    public void init(int startX, int startY, int startZ, int destX, int destY, int destZ,
            @NotNull PathSettings settings) {
        this.graph = settings.graph();

        this.successPredicate = settings.successPredicate();
        this.explorer = settings.explorer();
        this.heuristic = settings.heuristic();

        //indicate that we can start stepping
        state = State.INITIALIZED;
        success = false;

        //set the current node, g == 0
        current = new Node(startX, startY, startZ, 0, heuristic.distance(startX, startY, startZ, destX, destY,
                destZ), Movement.UNKNOWN, null);
        openSet.enqueue(current);
        best = current;

        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;

        this.destinationX = destX;
        this.destinationY = destY;
        this.destinationZ = destZ;
    }

    @Override
    public boolean step() {
        if (!openSet.isEmpty()) {
            current = openSet.dequeue();

            //Vec3IPredicate to avoid needing to create a Vec3I object
            //predicate returns true = we found our destination and have a path
            if (successPredicate.test(current.x, current.y, current.z, destinationX, destinationY, destinationZ)) {
                //complete (may throw an exception if already completed)
                best = current;
                complete(true);
                return true;
            }

            //reference the explore method: this is not strictly necessary, but it is cleaner, and prevents from
            //accidentally capturing a variable from step's scope, which could result in unnecessary object allocation
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

    private void complete(boolean success) {
        if (state == State.COMPLETE) {
            throw new IllegalStateException("Cannot complete already-completed path");
        }

        state = State.COMPLETE;
        this.success = success;
    }

    private void explore(Node current, Movement movementToNeighbor, int x, int y, int z) {
        Node neighbor = graph.computeIfAbsent(x, y, z, (x1, y1, z1) -> new Node(x1, y1, z1, Float.POSITIVE_INFINITY,
                heuristic.heuristic(x1, y1, z1, destinationX, destinationY, destinationZ), movementToNeighbor,
                null));

        float g = current.g + heuristic.distance(current.x, current.y, current.z, neighbor.x, neighbor.y, neighbor.z);
        if (g < neighbor.g) {
            neighbor.parent = current;
            neighbor.g = g;
            openSet.enqueueOrUpdate(neighbor);
        }
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
        if (state != State.COMPLETE) {
            throw new IllegalStateException("Can't compile a result while incomplete");
        }

        return new PathResult(best.reverseToVectorSet(), graph.size(), success);
    }

    @Override
    public void cleanup() {
        openSet.clear();
        openSet.trim(32);

        graph.clear();

        current = null;
        best = null;
        state = State.UNINITIALIZED;
    }
}