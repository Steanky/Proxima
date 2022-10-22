package com.github.steanky.proxima;

import com.github.steanky.vector.HashVec3I2ObjectMap;
import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3IPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BasicPathOperation implements PathOperation {
    private final NodeQueue openSet;
    private final HashVec3I2ObjectMap<Node> graph;

    private final Vec3IPredicate successPredicate;
    private final Explorer explorer;
    private final Heuristic heuristic;

    private State state;

    private Node current;
    private Node best;
    private PathResult result;

    private int startX;
    private int startY;
    private int startZ;

    private int destinationX;
    private int destinationY;
    private int destinationZ;

    public BasicPathOperation(@NotNull Vec3IPredicate successPredicate, @NotNull Explorer explorer,
            @NotNull Heuristic heuristic, @NotNull Vec3I spaceOrigin, @NotNull Vec3I spaceWidths) {
        this.openSet = new NodeQueue();
        this.graph = new HashVec3I2ObjectMap<>(spaceOrigin.x(), spaceOrigin.y(), spaceOrigin.z(), spaceWidths.x(),
                spaceWidths.y(), spaceWidths.z(), 32);
        this.successPredicate = Objects.requireNonNull(successPredicate);
        this.explorer = Objects.requireNonNull(explorer);
        this.heuristic = Objects.requireNonNull(heuristic);
        this.state = State.UNINITIALIZED;
    }

    @Override
    public void init(int startX, int startY, int startZ, int destinationX, int destinationY, int destinationZ) {
        //re-use the graph and map
        openSet.clear();
        openSet.trim(32);

        graph.clear();
        graph.trim(32);

        //indicate that we can start stepping
        state = State.INITIALIZED;

        //set the current node, g == 0
        current = new Node(startX, startY, startZ, 0, heuristic.distance(startX, startY, startZ, destinationX,
                destinationY, destinationZ), null);
        best = current;
        result = null;

        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;

        this.destinationX = destinationX;
        this.destinationY = destinationY;
        this.destinationZ = destinationZ;
    }

    @Override
    public void step() {
        if (!openSet.isEmpty()) {
            current = openSet.dequeue();

            //Vec3IPredicate to avoid needing to create a Vec3I object
            //predicate returns true = we found our destination and have a path
            if (successPredicate.test(current.x, current.y, current.z)) {
                //complete (may throw an exception if already completed)
                complete(current, true);
                return;
            }

            //reference the explore method: this is not strictly necessary, but it is cleaner, and prevents from
            //accidentally capturing a variable from step's scope, which would result in unnecessary object allocation
            explorer.exploreEach(current, this::explore);
            if (current.h < best.h) {
                best = current;
            }
        }
        else {
            complete(best, false);
        }
    }

    private void complete(Node best, boolean success) {
        if (state == State.COMPLETE) {
            throw new IllegalStateException("Cannot complete already-completed path");
        }

        state = State.COMPLETE;
        result = new PathResult(best.reverseToVectorList(), graph.size(), success);
    }

    private void explore(Node current, int x, int y, int z) {
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
                destinationZ), null);
    }

    @Override
    public @NotNull State state() {
        return state;
    }

    @Override
    public @NotNull PathResult result() {
        if (state != State.COMPLETE) {
            throw new IllegalStateException("Cannot get PathResult before path completion");
        }

        return result;
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
}