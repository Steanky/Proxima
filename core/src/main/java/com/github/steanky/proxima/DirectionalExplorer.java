package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DirectionalExplorer implements Explorer {
    private static final Direction[] DIRECTIONS = new Direction[] {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };

    private final DirectionalNodeSnapper nodeSnapper;

    public DirectionalExplorer(@NotNull DirectionalNodeSnapper nodeSnapper) {
        this.nodeSnapper = Objects.requireNonNull(nodeSnapper);
    }

    @Override
    public void exploreEach(@NotNull Node currentNode, int goalX, int goalY, int goalZ, @NotNull NodeHandler handler) {
        for (Direction direction : DIRECTIONS) {
            int dx = direction.x;
            int dz = direction.z;

            int nx = currentNode.x;
            int nz = currentNode.z;

            int tx = nx + dx;
            int tz = nz + dz;

            Node parent = currentNode.parent;
            if (parent != null) {
                if (tx == parent.x && tz == parent.z) {
                    //easy skip, don't re-visit our parent
                    continue;
                }
            }

            long value = nodeSnapper.snap(dx, dz, nx, currentNode.y, nz, currentNode.yOffset);
            if (value != DirectionalNodeSnapper.FAIL) {
                int height = DirectionalNodeSnapper.height(value);
                float offset = DirectionalNodeSnapper.offset(value);

                handler.handle(currentNode, tx, height, tz, offset);
            }
        }
    }
}
