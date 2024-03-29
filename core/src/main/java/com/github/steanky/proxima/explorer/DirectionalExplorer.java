package com.github.steanky.proxima.explorer;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.PathLimiter;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public abstract class DirectionalExplorer implements Explorer {
    protected final NodeSnapper snapper;
    private final Direction[] directions;
    private final PathLimiter limiter;

    public DirectionalExplorer(@NotNull Direction[] directions, @NotNull PathLimiter limiter, @NotNull NodeSnapper snapper) {
        this.directions = Arrays.copyOf(directions, directions.length);
        this.limiter = Objects.requireNonNull(limiter);
        this.snapper = Objects.requireNonNull(snapper);
    }

    @Override
    public void exploreEach(@NotNull Node current, @NotNull NodeHandler handler, @NotNull Vec3I2ObjectMap<Node> graph,
        int destinationX, int destinationY, int destinationZ) {
        if (!limiter.inBounds(current)) {
            //prune nodes that are not in bounds according to the limiter
            return;
        }

        int nx = current.x;
        int ny = current.y;
        int nz = current.z;

        int offsetIndex = startingDirectionIndex(current, destinationX, destinationY, destinationZ);

        /*
        if true, current.length is odd: we iterate forwards (clockwise around the compass N-E-S-W. if false we are even
        and iterate backwards (W-S-E-N); starting at an index chosen based on our position relative to the destination
        (we will explore nodes that are closer to our destination first)
         */
        boolean polarity = (current.length & 1) != 0;

        int start = polarity ? 0 : directions.length - 1;
        int limit = polarity ? directions.length : -1;
        int inc = polarity ? 1 : -1;

        if (polarity) offsetIndex--;
        else offsetIndex++;

        for (int i = start; i != limit; i += inc) {
            Direction direction = directions[Math.floorMod(i + offsetIndex, directions.length)];
            int dx = direction.x;
            int dy = direction.y;
            int dz = direction.z;

            //target x, y, z
            int tx = nx + dx;
            int ty = ny + dy;
            int tz = nz + dz;

            Node parent = current.parent;
            if (parent != null && isParent(parent, tx, ty, tz)) {
                //don't re-visit our parent, there's never a reason to do this
                continue;
            }

            Node neighborNode = graph.get(tx, ty, tz);
            if (neighborNode != null && current.g + 1 >= neighborNode.g) {
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

            handleDirection(direction, current, neighborNode, handler, graph);
        }
    }

    @Override
    public void exploreInitial(double startX, double startY, double startZ, @NotNull NodeInitializer initializer) {
        int isx = (int) Math.floor(startX);
        int isy = (int) Math.floor(startY);
        int isz = (int) Math.floor(startZ);

        long result = snapper.checkInitial(startX, startY, startZ, isx, isy, isz);
        if (result != NodeSnapper.FAIL) {
            //if we can reach the starting node, don't explore any other options
            int height = NodeSnapper.blockHeight(result);
            float blockOffset = NodeSnapper.blockOffset(result);
            float jumpOffset = NodeSnapper.jumpOffset(result);

            initializer.initialize(isx, height, isz, blockOffset, jumpOffset);
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

            result = snapper.checkInitial(startX, startY, startZ, inx, iny, inz);
            if (result != NodeSnapper.FAIL) {
                //valid, reachable, alternate starting node, initialize it
                int height = NodeSnapper.blockHeight(result);
                float blockOffset = NodeSnapper.blockOffset(result);
                float jumpOffset = NodeSnapper.jumpOffset(result);

                initializer.initialize(inx, height, inz, blockOffset, jumpOffset);
            }
        }
    }

    protected abstract int startingDirectionIndex(@NotNull Node current, int destinationX, int destinationY,
            int destinationZ);

    protected abstract boolean isParent(@NotNull Node parent, int tx, int ty, int tz);

    protected abstract void handleDirection(@NotNull Direction direction, @NotNull Node currentNode, @Nullable Node neighborNode, @NotNull NodeHandler handler, @NotNull Vec3I2ObjectMap<Node> graph);
}
