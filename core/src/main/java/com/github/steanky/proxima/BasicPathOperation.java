package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BasicPathOperation implements PathOperation {
    private final NodeQueue openSet;
    private final Vec3IPredicate successPredicate;
    private final Explorer explorer;
    private final Heuristic heuristic;
    private final Vec3I destination;

    private final Vec3I2ObjectMap<Node> graph;

    private Node current;
    private Node best;
    private State state;
    private PathResult result;

    public BasicPathOperation(@NotNull Vec3IPredicate successPredicate, @NotNull Explorer explorer,
            @NotNull Heuristic heuristic, @NotNull Vec3I destination, @NotNull Vec3I2ObjectMap<Node> graph) {
        this.openSet = new NodeQueue();
        this.successPredicate = Objects.requireNonNull(successPredicate);
        this.explorer = Objects.requireNonNull(explorer);
        this.heuristic = Objects.requireNonNull(heuristic);
        this.destination = destination.immutable();
        this.graph = Objects.requireNonNull(graph);
        this.state = State.IN_PROGRESS;
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
        state = success ? State.SUCCEEDED : State.FAILED;
        result = new PathResult(best.reverse(), graph.size(), success);
    }

    private void explore(Node current, int x, int y, int z) {
        Node neighbor = graph.computeIfAbsent(x, y, z, (x1, y1, z1) -> new Node(x1, y1, z1, Float.POSITIVE_INFINITY,
                heuristic.heuristic(x1, y1, z1, destination.x(), destination.y(), destination.z()), null));

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
    public @NotNull Vec3I start() {
        return null;
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
