package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.snapper.DirectionalNodeSnapper;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WalkExplorer implements Explorer {
    private static final Direction[] DIRECTIONS = new Direction[] {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    private final DirectionalNodeSnapper nodeSnapper;
    private final PathLimiter limiter;

    public WalkExplorer(@NotNull DirectionalNodeSnapper nodeSnapper, @NotNull PathLimiter limiter) {
        this.nodeSnapper = Objects.requireNonNull(nodeSnapper);
        this.limiter = Objects.requireNonNull(limiter);
    }

    @Override
    public void exploreEach(@NotNull Node currentNode, int goalX, int goalY, int goalZ, @NotNull NodeHandler handler,
            @NotNull Vec3I2ObjectMap<Node> graph) {
        if (!limiter.inBounds(currentNode)) {
            //prune nodes that are not in bounds according to the limiter
            return;
        }

        for (Direction direction : DIRECTIONS) {
            int dx = direction.x;
            int dz = direction.z;

            int nx = currentNode.x;
            int ny = currentNode.y;
            int nz = currentNode.z;

            int tx = nx + dx;
            int tz = nz + dz;

            Node parent = currentNode.parent;
            if (parent != null && tx == parent.x && tz == parent.z) {
                //fast skip, don't re-visit our parent
                continue;
            }

            Node neighborNode = graph.get(tx, ny, tz);
            if (neighborNode != null && currentNode.g + 1 >= neighborNode.g) {
                /*
                ignore travel to nodes that

                a) we already visited
                b) the path when going from current to this node will be longer than (or equal to) the path to the other
                node

                we can add 1 to the current node's g, because it is the MINIMUM additional path length; we may determine
                that it goes larger after snapping, but it doesn't change this calculation if it does
                 */
                continue;
            }

            long value = nodeSnapper.snap(direction, nx, ny, nz, currentNode.yOffset);
            if (value != DirectionalNodeSnapper.FAIL) {
                int height = DirectionalNodeSnapper.height(value);
                float offset = DirectionalNodeSnapper.offset(value);

                if (height != ny) {
                    /*
                    the actual y of the node differs from what we guessed previously. we could re-check its g-value
                    here, but the handler will do that as a matter of course, and we already snapped (the expensive
                    operation); so don't bother. we grab the neighbor node in the first place because the handler will
                    expect us to provide it if we can
                     */
                    neighborNode = graph.get(tx, height, tz);
                }

                handler.handle(currentNode, neighborNode, tx, height, tz, offset);
            }
        }
    }
}
