package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WalkNodeSnapper implements DirectionalNodeSnapper {
    private static final double EPSILON = 1E-6;

    private final double width;
    private final double halfWidth;
    private final double height;

    private final boolean full;

    private final int blockHeight;
    private final int halfBlockWidth;

    private final Vec3I[] hDeltas;
    private final Vec3I[] vDeltas;

    private final double fallTolerance;
    private final double jumpHeight;
    private final Space space;

    private final Bounds3I searchArea;

    public WalkNodeSnapper(double width, double height, double fallTolerance, double jumpHeight, @NotNull Space space,
            @NotNull Bounds3I searchArea) {
        if (width < 0 || height < 0 || fallTolerance < 0 || jumpHeight < 0 || !Double.isFinite(width) ||
                !Double.isFinite(height)) {
            throw new IllegalArgumentException("Invalid parameter");
        }

        this.width = width;
        this.halfWidth = width / 2;
        this.height = height;

        this.full = width == Math.rint(width);

        int blockWidth = ((int)Math.ceil(width)) | 1;
        this.blockHeight = (int)Math.ceil(height);

        this.halfBlockWidth = blockWidth >> 1;

        this.hDeltas = new Vec3I[blockWidth * blockHeight];
        this.vDeltas = new Vec3I[blockWidth * blockWidth];

        for (int h = 0; h < blockHeight; h++) {
            for (int w = 0; w < blockWidth; w++) {
                //this is effectively a two-dimensional vector, z is always 0
                //how this is interpreted depends on the direction
                //these vectors are small enough that, unless we have really large entities, they will be cached
                hDeltas[h * blockHeight + w] = Vec3I.immutable(w - halfBlockWidth, h, 0);
            }
        }

        for (int x = 0; x < blockWidth; x++) {
            for (int z = 0; z < blockWidth; z++) {
                //vertical deltas are simple
                vDeltas[x * blockWidth + z] = Vec3I.immutable(x - halfBlockWidth, 0, z - halfBlockWidth);
            }
        }

        this.fallTolerance = fallTolerance;
        this.jumpHeight = jumpHeight + EPSILON;
        this.space = Objects.requireNonNull(space);
        this.searchArea = Objects.requireNonNull(searchArea);
    }

    @Override
    public void snap(@NotNull Direction direction, @NotNull Node node, @NotNull NodeHandler handler) {
        int dx = direction.x;
        int dy = direction.y;
        int dz = direction.z;

        int nx = node.x + dx;
        int ny = node.y + dy;
        int nz = node.z + dz;

        if (!searchArea.contains(nx, ny, nz)) {
            //fast exist: don't check out of bounds
            return;
        }

        int hx = 0;
        int hy = 0;
        int hz = 0;

        double highestY = Double.NEGATIVE_INFINITY;
        Solid highestSolid = null;

        //first, check the blocks in front of us
        for (Vec3I delta : hDeltas) {
            int dh = delta.x(); //translation along the axis perpendicular to the direction
            int dv = delta.y(); //translation above the agent

            if (!full) {
                //first block we may encounter
                int x = dx == 0 ? nx + dh : node.x + (dx * halfBlockWidth);
                int y = ny + dv;
                int z = dz == 0 ? nz + dh : node.z + (dz * halfBlockWidth);

                Solid solid = space.solidAt(x, y, z);

                //if the solid is partial, we might not already be overlapping it
                //if the solid is full, we know we're overlapping it
                //if the solid is empty, it has no collision
                if (!solid.isEmpty() && !solid.isFull()) {
                    Bounds3D bounds = solid.bounds();

                    double box = x + bounds.originX();
                    double boz = z + bounds.originZ();

                    double aox = node.x + 0.5D - halfWidth;
                    double aoz = node.z + 0.5D - halfWidth;

                    if (!overlaps(box, boz, bounds.lengthX(), bounds.lengthZ(), aox, aoz, width, width)) {
                        double height = y + bounds.lengthY() - node.y;

                        if (height > jumpHeight) {
                            //fast exit: can't jump over this block
                            return;
                        }

                        //track the highest solid this layer
                        if (height > highestY) {
                            highestY = height;
                            highestSolid = solid;

                            hx = x;
                            hy = y;
                            hz = z;
                        }
                    }
                }
            }

            //second block we might encounter (first block, if full)
            int x = dx == 0 ? nx + dh : nx + (dx * halfBlockWidth);
            int y = ny + dv;
            int z = dz == 0 ? nz + dh : nz + (dz * halfBlockWidth);

            Solid solid = space.solidAt(x, y, z);
            if (solid.isEmpty()) {
                //nothing to do this iteration if empty
                continue;
            }

            if (!solid.isFull()) {
                //more detailed check necessary, we might not collide with this solid
                Bounds3D bounds = solid.bounds();

                double box = x + bounds.originX();
                double boz = z + bounds.originZ();

                double aox = nx + 0.5D - halfWidth;
                double aoz = nz + 0.5D - halfWidth;

                if (overlaps(box, boz, bounds.lengthX(), bounds.lengthZ(), aox, aoz, width, width)) {

                }
            }
        }
    }

    //checks for overlap (in two dimensions); applies an epsilon value
    private static boolean overlaps(double ox1, double oz1, double lx1, double lz1,
            double ox2, double oz2, double lx2, double lz2) {
        return false;
    }

    @Override
    public boolean canSkip(@NotNull Node currentNode, @NotNull Node parent, @NotNull Direction direction) {
        int dX = parent.x - currentNode.x;
        int dZ = parent.z - currentNode.z;

        return Math.signum(dX) == direction.x && Math.signum(dZ) == direction.z;
    }
}
