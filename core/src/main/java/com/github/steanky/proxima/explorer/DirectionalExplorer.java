package com.github.steanky.proxima.explorer;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.PathLimiter;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.node.NodeQueue;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public abstract class DirectionalExplorer implements Explorer {
    private final Direction[] directions;
    private final PathLimiter limiter;

    protected final NodeSnapper snapper;

    public DirectionalExplorer(@NotNull Direction[] directions, @NotNull PathLimiter limiter,
            @NotNull NodeSnapper snapper) {
        this.directions = Arrays.copyOf(directions, directions.length);
        this.limiter = Objects.requireNonNull(limiter);
        this.snapper = Objects.requireNonNull(snapper);
    }

    @Override
    public void exploreEach(@NotNull Node currentNode, @NotNull NodeHandler handler,
            @NotNull Vec3I2ObjectMap<Node> graph) {
        if (!limiter.inBounds(currentNode)) {
            //prune nodes that are not in bounds according to the limiter
            return;
        }

        int nx = currentNode.x;
        int ny = currentNode.y;
        int nz = currentNode.z;

        for (Direction direction : directions) {
            int dx = direction.x;
            int dy = direction.y;
            int dz = direction.z;

            //target x, y, z
            int tx = nx + dx;
            int ty = ny + dy;
            int tz = nz + dz;

            Node parent = currentNode.parent;
            if (parent != null && isParent(parent, tx, ty, tz)) {
                //don't re-visit our parent, there's never a reason to do this
                continue;
            }

            Node neighborNode = graph.get(tx, ty, tz);
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

            handleDirection(direction, currentNode, neighborNode, handler, graph);
        }
    }

    @Override
    public void exploreInitial(double startX, double startY, double startZ, @NotNull NodeInitializer initializer) {
        int isx = (int) Math.floor(startX);
        int isy = (int) Math.floor(startY);
        int isz = (int) Math.floor(startZ);

        float i = snapper.checkInitial(startX, startY, startZ, isx, isy, isz);
        if (!Float.isNaN(i)) {
            //if we can reach the starting node, don't explore any other options
            initializer.initialize(isx, isy, isz, i);
            return;
        }

        //we can't reach our starting node - try other directions
        for (Direction direction : directions) {
            double nx = startX + direction.x;
            double ny = startY + direction.y;
            double nz = startZ + direction.z;

            int inx = (int) Math.floor(nx);
            int iny = (int) Math.floor(ny);
            int inz = (int) Math.floor(nz);

            double distance = Vec3D.distanceSquared(startX, startY, startZ, inx, iny, inz);

            //checkInitial won't expect distances greater than 1
            if (distance > 1) {
                continue;
            }

            i = snapper.checkInitial(startX, startY, startZ, inx, iny, inz);
            if (!Float.isNaN(i)) {
                //valid, reachable, alternate starting node, initialize it
                initializer.initialize(inx, iny, inz, i);
            }
        }
    }

    protected abstract boolean isParent(@NotNull Node parent, int tx, int ty, int tz);

    protected abstract void handleDirection(@NotNull Direction direction, @NotNull Node currentNode,
            @Nullable Node neighborNode, @NotNull NodeHandler handler, @NotNull Vec3I2ObjectMap<Node> graph);
}
