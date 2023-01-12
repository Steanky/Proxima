package com.github.steanky.proxima.explorer;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.PathLimiter;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlightExplorer extends DirectionalExplorer {
    private static final Direction[] DIRECTIONS = new Direction[] {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.UP,
            Direction.DOWN
    };

    public FlightExplorer(@NotNull PathLimiter limiter, @NotNull NodeSnapper snapper) {
        super(DIRECTIONS, limiter, snapper);
    }

    @Override
    protected boolean isParent(@NotNull Node parent, int tx, int ty, int tz) {
        return parent.x == tx && parent.y == ty && parent.z == tz;
    }

    @Override
    protected void handleDirection(@NotNull Direction direction, @NotNull Node currentNode, @Nullable Node neighborNode,
            @NotNull NodeHandler handler, @NotNull Vec3I2ObjectMap<Node> graph) {
        int tx = currentNode.x + direction.x;
        int ty = currentNode.y + direction.y;
        int tz = currentNode.z + direction.z;

        long result = snapper.snap(direction, currentNode.x, currentNode.y, currentNode.z, currentNode.blockOffset);
        if (result != NodeSnapper.FAIL) {
            float offset = NodeSnapper.blockOffset(result);
            handler.handle(currentNode, neighborNode, tx, ty, tz, offset, 0);
        }
    }
}
