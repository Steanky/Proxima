package com.github.steanky.proxima.path;

import com.github.steanky.proxima.Heuristic;
import com.github.steanky.proxima.explorer.Explorer;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.node.NodeProcessor;
import com.github.steanky.proxima.node.NodeQueue;
import com.github.steanky.vector.Vec3I;
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
    public void init(double startX, double startY, double startZ, int destX, int destY, int destZ, @NotNull PathSettings settings) {
        this.graph = settings.graph();

        this.successPredicate = settings.successPredicate();
        this.explorer = settings.explorer();
        this.heuristic = settings.heuristic();
        this.nodeProcessor = settings.nodeProcessor();

        this.destinationX = destX;
        this.destinationY = destY;
        this.destinationZ = destZ;

        //find the starting node(s)
        //this may populate openSet and graph with a few values to start
        this.explorer.exploreInitial(startX, startY, startZ, this::initialize);

        //if nothing was added to openSet, we have no valid starting position
        //this operation will complete with an unsuccessful path result
        best = current = openSet.isEmpty() ? null : openSet.first();

        //indicate that we can start stepping
        state = State.INITIALIZED;
        success = false;
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

    @Override
    public @NotNull PathResult makeResult() {
        if (state != State.COMPLETE) {
            throw new IllegalStateException("Can't compile a result while incomplete");
        }

        if (best == null) {
            //if best is null, we don't even have a starting node, and so have not explored anything
            return PathResult.EMPTY;
        }

        nodeProcessor.processPath(best, graph);
        return new PathResult(best == null ? null : best.reverse(), graph.size(), success);
    }

    @Override
    public void cleanup() {
        openSet.clear();
        openSet.trim(NodeQueue.DEFAULT_INITIAL_CAPACITY);

        if (graph != null) {
            graph.clear();
        }

        current = null;
        best = null;
        state = State.UNINITIALIZED;
    }

    private void complete(boolean success) {
        if (state == State.COMPLETE) {
            throw new IllegalStateException("Cannot complete already-completed path");
        }

        state = State.COMPLETE;
        this.success = success;
    }

    private void initialize(int x, int y, int z, float blockOffset, float jumpOffset) {
        Node node = new Node(x, y, z, 0, heuristic.heuristic(x, y, z, destinationX, destinationY, destinationZ),
                blockOffset, jumpOffset);
        graph.put(x, y, z, node);
        openSet.enqueue(node);
    }

    private void explore(Node current, Node target, int x, int y, int z, float blockOffset, float jumpOffset) {
        if (target == null) {
            target = new Node(x, y, z, Float.POSITIVE_INFINITY,
                    heuristic.heuristic(x, y, z, destinationX, destinationY, destinationZ), blockOffset, jumpOffset);
            graph.put(x, y, z, target);
        }

        float g = current.g + (float) Vec3I.distanceSquared(current.x, current.y, current.z, x, y, z);
        if (g < target.g) {
            target.parent = current;
            target.g = g;
            openSet.enqueueOrUpdate(target);
        }
    }
}