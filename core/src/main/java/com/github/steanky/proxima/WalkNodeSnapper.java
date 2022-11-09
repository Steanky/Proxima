package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WalkNodeSnapper implements DirectionalNodeSnapper {
    private final double width;
    private final double halfWidth;
    private final double height;

    private final int searchHeight;

    private final double epsilon;

    private final boolean fullWidth;
    private final boolean fullHeight;

    private final int halfBlockWidth;

    private final Vec3I[] vDeltas;

    private final double fallTolerance;
    private final double jumpHeight;
    private final Space space;

    private final Bounds3I searchArea;

    public WalkNodeSnapper(double width, double height, double fallTolerance, double jumpHeight, @NotNull Space space,
            @NotNull Bounds3I searchArea, double epsilon) {
        check(width, height, fallTolerance, jumpHeight, epsilon);

        this.width = width;
        this.halfWidth = width / 2;
        this.height = height;

        this.epsilon = epsilon;

        this.fullWidth = width == Math.rint(width);
        this.fullHeight = height == Math.rint(height);

        int blockWidth = ((int)Math.ceil(width)) | 1;
        this.searchHeight = (int)Math.ceil(height + jumpHeight);

        this.halfBlockWidth = blockWidth >> 1;

        this.vDeltas = new Vec3I[blockWidth * blockWidth];
        for (int x = 0; x < blockWidth; x++) {
            for (int z = 0; z < blockWidth; z++) {
                vDeltas[x * blockWidth + z] = Vec3I.immutable(x - halfBlockWidth, 0, z - halfBlockWidth);
            }
        }

        this.fallTolerance = fallTolerance;
        this.jumpHeight = jumpHeight;
        this.space = Objects.requireNonNull(space);
        this.searchArea = Objects.requireNonNull(searchArea);
    }

    private static void check(double width, double height, double fallTolerance, double jumpHeight, double epsilon) {
        if (width < 0 || !Double.isFinite(width)) {
            //width must be non-negative and finite
            throw new IllegalArgumentException("Invalid width: " + width);
        }

        if (height < 0 || !Double.isFinite(height)) {
            //height must be non-negative and finite
            throw new IllegalArgumentException("Invalid height: " + height);
        }

        if (fallTolerance < 0 || Double.isNaN(fallTolerance)) {
            //fallTolerance must be non-negative and not a NaN (positive infinity is allowed)
            throw new IllegalArgumentException("Invalid fallTolerance: " + fallTolerance);
        }

        if (jumpHeight < 0 || Double.isNaN(jumpHeight)) {
            //jumpHeight must be non-negative and not a NaN (positive infinity is allowed)
            throw new IllegalArgumentException("Invalid jumpHeight: " + jumpHeight);
        }

        if (epsilon < 0 || !Double.isFinite(epsilon)) {
            //epsilon must be positive and finite
            throw new IllegalArgumentException("Invalid epsilon: " + epsilon);
        }
    }

    @Override
    public void snap(@NotNull Direction direction, @NotNull Node node, @NotNull NodeHandler handler) {
        int dx = direction.x;
        int dz = direction.z;

        int nx = node.x + dx;
        int nz = node.z + dz;

        //fast exist: don't check out of bounds
        if (!searchArea.contains(nx, node.y, nz)) {
            return;
        }

        double highestY = node.y;
        double newHeight = Double.NaN;
        for (int i = 0; i < searchHeight; i++) {
            int y = node.y + i; //y of the solid

            Solid tallestSolid = Solid.EMPTY;
            for (int dh = -halfBlockWidth; dh <= halfBlockWidth; dh++) {
                //full agents don't need to check for collisions inside themselves
                if (!fullWidth) {
                    //first block we may encounter
                    int x = dx == 0 ? nx + dh : node.x + (dx * halfBlockWidth);
                    int z = dz == 0 ? nz + dh : node.z + (dz * halfBlockWidth);

                    Solid solid = space.solidAt(x, y, z);

                    //if the solid is full, we know we're overlapping it
                    //if the solid is empty, it has no collision
                    //if the solid is partial, check if we're overlapping
                    if (!solid.isEmpty() && !solid.isFull()) {
                        Bounds3D bounds = solid.bounds();

                        double box = x + bounds.originX();
                        double boz = z + bounds.originZ();

                        double aox = node.x + 0.5D - halfWidth;
                        double aoz = node.z + 0.5D - halfWidth;

                        if (!overlaps(box, boz, bounds.lengthX(), bounds.lengthZ(), aox, aoz, width, width, epsilon)) {
                            double requiredJumpHeight = y + bounds.lengthY() - node.y;

                            //fast exit: can't jump over this block
                            if (requiredJumpHeight > jumpHeight) {
                                return;
                            }

                            if (bounds.lengthY() > tallestSolid.bounds().lengthY()) {
                                tallestSolid = solid;
                            }
                        }
                    }
                }

                //second block we might encounter (first block, if full)
                int x = dx == 0 ? nx + dh : nx + (dx * halfBlockWidth);
                int z = dz == 0 ? nz + dh : nz + (dz * halfBlockWidth);

                Solid solid = space.solidAt(x, y, z);

                if (solid.isEmpty()) {
                    continue;
                }

                //simpler check if the solid is full, we don't need to test overlap
                if (solid.isFull()) {
                    double requiredJumpHeight = y + 1 - node.y;
                    if (requiredJumpHeight > jumpHeight) {
                        return;
                    }

                    tallestSolid = solid;
                    continue;
                }

                //more detailed check necessary, we might not collide with this solid
                Bounds3D bounds = solid.bounds();

                double box = x + bounds.originX();
                double boz = z + bounds.originZ();

                double aox = node.x + 0.5D - halfWidth;
                double aoz = node.z + 0.5D - halfWidth;

                double awx = width + Math.abs(dx);
                double awz = width + Math.abs(dz);

                //directionally expand the bounding box in the direction we're traveling
                if (dx < 0) {
                    aox += dx;
                }

                if (dz < 0) {
                    aoz += dz;
                }

                //if the directionally-expanded bounds overlaps, we have a collision
                if (overlaps(box, boz, bounds.lengthX(), bounds.lengthZ(), aox, aoz, awx, awz, epsilon)) {
                    double requiredJumpHeight = y + bounds.lengthY() - node.y;
                    if (requiredJumpHeight > jumpHeight) {
                        return;
                    }

                    if (bounds.lengthY() > tallestSolid.bounds().lengthY()) {
                        tallestSolid = solid;
                    }
                }
            }

            if (!tallestSolid.isEmpty()) {
                double highestLayerY = y + tallestSolid.bounds().lengthY();
                if (highestLayerY > highestY) {
                    highestY = highestLayerY;
                }
            }

            if ((tallestSolid.isEmpty() ? y + 1 : tallestSolid.bounds().originY() - highestY) >= height) {
                //found somewhere we can fit
                newHeight = highestY;
                break;
            }
        }

        //newHeight was never assigned, so we can't move this direction
        if (Double.isNaN(newHeight)) {
            return;
        }

        //jumping is necessary, we need to check above us
        if (newHeight > node.y) {
            double requiredJump = newHeight - node.y;

            return;
        }
    }

    private static boolean overlaps(double ox1, double oz1, double lx1, double lz1,
            double ox2, double oz2, double lx2, double lz2, double e) {
        return false;
    }

    @Override
    public boolean canSkip(@NotNull Node currentNode, @NotNull Node parent, @NotNull Direction direction) {
        int dX = parent.x - currentNode.x;
        int dZ = parent.z - currentNode.z;

        return Math.signum(dX) == direction.x && Math.signum(dZ) == direction.z;
    }
}
