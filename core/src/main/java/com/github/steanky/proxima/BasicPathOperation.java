package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BasicPathOperation implements PathOperation {
    private final NodeQueue openSet;
    private final Vec3IPredicate successPredicate;
    private final Explorer explorer;
    private final Heuristic heuristic;
    private final Vec3I2ObjectMap<Node> graph;

    private Node current;
    private Node best;

    private State state;
    private PathResult result;

    private int startX;
    private int startY;
    private int startZ;

    private int destinationX;
    private int destinationY;
    private int destinationZ;

    public BasicPathOperation(@NotNull Vec3IPredicate successPredicate, @NotNull Explorer explorer,
            @NotNull Heuristic heuristic, @NotNull Vec3I2ObjectMap<Node> graph) {
        this.openSet = new NodeQueue();
        this.successPredicate = Objects.requireNonNull(successPredicate);
        this.explorer = Objects.requireNonNull(explorer);
        this.heuristic = Objects.requireNonNull(heuristic);
        this.graph = Objects.requireNonNull(graph);
        this.state = State.UNINITIALIZED;
    }

    @Override
    public void init(int startX, int startY, int startZ, int destinationX, int destinationY, int destinationZ) {
        current = new Node(startX, startY, startZ, 0, heuristic.distance(startX, startY, startZ, destinationX,
                destinationY, destinationZ), null);
        best = current;

        state = State.INITIALIZED;
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

            if (successPredicate.test(current.x, current.y, current.z)) {
                complete(true, current);
                return;
            }

            explorer.exploreEach(current, this::explore);
            if (current.h < best.h) {
                best = current;
            }
        }
        else {
            complete(false, best);
        }
    }

    private void complete(boolean success, Node best) {
        if (state == State.COMPLETE) {
            throw new IllegalStateException("Cannot complete already-completed path");
        }

        state = State.COMPLETE;
        result = new PathResult(best.reverse(), graph.size(), success);
    }

    private void explore(Node current, int x, int y, int z) {
        Node neighbor = graph.computeIfAbsent(x, y, z, (x1, y1, z1) -> new Node(x1, y1, z1, Float.POSITIVE_INFINITY,
                heuristic.heuristic(x1, y1, z1, destinationX, destinationY, destinationZ), null));

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
