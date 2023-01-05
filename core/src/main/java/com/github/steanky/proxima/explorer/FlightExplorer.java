package com.github.steanky.proxima.explorer;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.PathLimiter;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FlightExplorer extends DirectionalExplorer {
    private static final Direction[] DIRECTIONS = new Direction[] {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.UP,
            Direction.DOWN
    };

    private final Space space;

    public FlightExplorer(@NotNull PathLimiter limiter, @NotNull Space space) {
        super(DIRECTIONS, limiter);
        this.space = Objects.requireNonNull(space);
    }

    @Override
    protected boolean isParent(@NotNull Node other, int tx, int ty, int tz) {
        return other.x == tx && other.y == ty && other.z == tz;
    }

    @Override
    protected void handleDirection(@NotNull Direction direction, @NotNull Node currentNode, @Nullable Node neighborNode,
            @NotNull NodeHandler handler, @NotNull Vec3I2ObjectMap<Node> graph) {
        int tx = currentNode.x + direction.x;
        int ty = currentNode.y + direction.y;
        int tz = currentNode.z + direction.z;


    }
}
