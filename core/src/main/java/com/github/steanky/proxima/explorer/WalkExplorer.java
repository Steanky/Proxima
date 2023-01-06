package com.github.steanky.proxima.explorer;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.PathLimiter;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class WalkExplorer extends DirectionalExplorer {
    private static final Direction[] DIRECTIONS = new Direction[] {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    private final NodeSnapper nodeSnapper;

    public WalkExplorer(@NotNull NodeSnapper nodeSnapper, @NotNull PathLimiter limiter) {
        super(DIRECTIONS, limiter);
        this.nodeSnapper = Objects.requireNonNull(nodeSnapper);
    }

    @Override
    protected boolean isParent(@NotNull Node parent, int tx, int ty, int tz) {
        return tx == parent.x && tz == parent.z;
    }

    @Override
    protected void handleDirection(@NotNull Direction direction, @NotNull Node currentNode, @Nullable Node neighborNode,
            @NotNull NodeHandler handler, @NotNull Vec3I2ObjectMap<Node> graph) {
        int nx = currentNode.x;
        int ny = currentNode.y;
        int nz = currentNode.z;

        long value = nodeSnapper.snap(direction, nx, ny, nz, currentNode.yOffset);
        if (value != NodeSnapper.FAIL) {
            int height = NodeSnapper.height(value);
            float offset = NodeSnapper.offset(value);

            int tx = nx + direction.x;
            int tz = nz + direction.z;

            if (height != ny) {
                /*
                the actual y of the node differs from what we guessed previously. we could re-check its g-value here,
                but the handler will do that as a matter of course, and we already snapped (the expensive operation);
                so don't bother. we grab the neighbor node in the first place because the handler will expect us to
                provide it if we can
                */
                neighborNode = graph.get(tx, height, tz);
            }

            handler.handle(currentNode, neighborNode, tx, height, tz, offset);
        }
    }
}
