package com.github.steanky.proxima.explorer;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.PathLimiter;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WalkExplorer extends DirectionalExplorer {
    private static final Direction[] DIRECTIONS =
            new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static final int[] START_LOOKUP = new int[] {
            3,
            1,
            0,
            2
    };

    public WalkExplorer(@NotNull NodeSnapper snapper, @NotNull PathLimiter limiter) {
        super(DIRECTIONS, limiter, snapper);
    }

    @Override
    protected int startingDirectionIndex(@NotNull Node current, int destinationX, int destinationY, int destinationZ) {
        return START_LOOKUP[key(destinationX - current.x, destinationZ - current.z)];
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

        long value = snapper.snap(direction, nx, ny, nz, currentNode.blockOffset);
        if (value == NodeSnapper.FAIL) {
            return;
        }

        int height = NodeSnapper.blockHeight(value);
        float blockOffset = NodeSnapper.blockOffset(value);
        float jumpOffset = NodeSnapper.jumpOffset(value);

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

        handler.handle(currentNode, neighborNode, tx, height, tz, blockOffset, jumpOffset);
    }

    private static int key(int dx, int dz) {
        return (shift(dx) << 1) | shift(dz);
    }

    private static int shift(int v) {
        return v >>> (Integer.SIZE - 1);
    }
}
