package com.github.steanky.proxima.path;

import com.github.steanky.proxima.Heuristic;
import com.github.steanky.proxima.explorer.Explorer;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.node.NodeProcessor;
import com.github.steanky.proxima.node.NodeQueue;
import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IBiPredicate;
import org.jetbrains.annotations.NotNull;

public class BasicPathOperation implements PathOperation {
    private final NodeQueue openSet;

    private Vec3I2ObjectMap<Node> graph;

    private Vec3IBiPredicate successPredicate;
    private Explorer explorer;
    private Heuristic heuristic;
    private NodeProcessor nodeProcessor;

    private State state;
    private boolean success;

    private Node current;
    private Node best;

    private int destinationX;
    private int destinationY;
    private int destinationZ;

    public BasicPathOperation() {
        this.openSet = new NodeQueue();
        this.state = State.UNINITIALIZED;
    }

    @Override
    public void init(int startX, int startY, int startZ, int destX, int destY, int destZ,
            @NotNull PathSettings settings, float yOffset) {
        this.graph = settings.graph();

        this.successPredicate = settings.successPredicate();
        this.explorer = settings.explorer();
        this.heuristic = settings.heuristic();
        this.nodeProcessor = settings.nodeProcessor();

        //indicate that we can start stepping
        state = State.INITIALIZED;
        success = false;

        //set the current node, g == 0
        current = new Node(startX, startY, startZ, 0, heuristic.distance(startX, startY, startZ, destX, destY,
                destZ), null, yOffset);
        openSet.enqueue(current);
        graph.put(startX, startY, startZ, current);
        best = current;

        this.destinationX = destX;
        this.destinationY = destY;
        this.destinationZ = destZ;
    }

    @Override
    public boolean step() {
        if (openSet.isEmpty()) {
            complete(false);
            return true;
        }

        current = openSet.dequeue();

        //Vec3IBiPredicate to avoid needing to create a Vec3I object
        //predicate returns true = we found our destination and have a path
        if (successPredicate.test(current.x, current.y, current.z, destinationX, destinationY, destinationZ)) {
            //complete (may throw an exception if already completed)
            best = current;
            complete(true);
            return true;
        }

        //reference the explore method: this is not strictly necessary, but it is cleaner, and prevents from
        //accidentally capturing a variable from step's scope
        explorer.exploreEach(current, this::explore, graph);
        if (current.h < best.h) {
            best = current;
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

    private void explore(Node current, Node target, int x, int y, int z, float yOffset) {
        if (target == null) {
            target = new Node(x, y, z, Float.POSITIVE_INFINITY,
                    heuristic.heuristic(x, y, z, destinationX, destinationY, destinationZ), null, yOffset);
            graph.put(x, y, z, target);
        }

        float g = current.g + heuristic.distance(current.x, current.y, current.z, x, y, z);
        if (g < target.g) {
            target.parent = current;
            target.g = g;
            openSet.enqueueOrUpdate(target);
        }
    }

    @Override
    public @NotNull PathResult makeResult() {
        if (state != State.COMPLETE) {
            throw new IllegalStateException("Can't compile a result while incomplete");
        }

        Node target = nodeProcessor.processPath(best, graph);
        return new PathResult(target.reverseToNavigationList(), graph.size(), success);
    }

    @Override
    public void cleanup() {
        openSet.clear();
        openSet.trim(NodeQueue.DEFAULT_INITIAL_CAPACITY);

        graph.clear();

        current = null;
        best = null;
        state = State.UNINITIALIZED;
    }
}