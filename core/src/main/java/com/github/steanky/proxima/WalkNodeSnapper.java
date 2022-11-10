package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Bounds3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WalkNodeSnapper implements DirectionalNodeSnapper {
    private final double width;
    private final double halfWidth;
    private final double wDiff;
    private final double height;

    private final int ceilHeight;

    private final int searchHeight;
    private final int fallSearchHeight;

    private final boolean fullWidth;
    private final boolean fullHeight;

    private final int halfBlockWidth;

    private final double jumpHeight;
    private final Space space;

    private final Bounds3I searchArea;

    public WalkNodeSnapper(double width, double height, double fallTolerance, double jumpHeight, @NotNull Space space,
            @NotNull Bounds3I searchArea, double epsilon) {
        check(width, height, fallTolerance, jumpHeight, epsilon);

        this.width = width;
        this.halfWidth = width / 2;
        this.wDiff = (Math.ceil(width) - width) / 2;
        this.height = height;
        this.ceilHeight = (int)Math.ceil(height);

        this.fullWidth = width == Math.rint(width);
        this.fullHeight = height == Math.rint(height);

        int blockWidth = ((int)Math.ceil(width)) | 1;
        this.searchHeight = (int)Math.ceil(height + jumpHeight);
        this.fallSearchHeight = (int)Math.ceil(fallTolerance);

        this.halfBlockWidth = blockWidth >> 1;

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

        double yOffset = node.yOffset;
        double exactY = node.y + yOffset;
        double newY = Double.NaN;

        for (int i = 0; i < searchHeight; i++) {
            int y = node.y + i; //y of the solid

            Solid tallestSolid = Solid.EMPTY;

            //search an individual layer
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

                        //if we're overlapping, we don't have any collision
                        if (overlaps(x, z, bounds, node)) {
                            continue;
                        }

                        double ex = dx * wDiff;
                        double ez = dz * wDiff;

                        //if we overlap the expanded bounding box, we do have a collision
                        if (expandOverlaps(x, z, ex, ez, bounds, node)) {
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

                //nothing to check if empty
                if (solid.isEmpty()) {
                    continue;
                }

                //simpler check if the solid is full, we don't need to test overlap
                if (solid.isFull()) {
                    double requiredJumpHeight = y + 1 - (node.y + yOffset);
                    if (requiredJumpHeight > jumpHeight) {
                        return;
                    }

                    tallestSolid = solid;
                    continue;
                }

                Bounds3D bounds = solid.bounds();

                //if the directionally-expanded bounds overlaps, we have a collision
                if (expandOverlaps(x, z, dx, dz, bounds, node)) {
                    double requiredJumpHeight = y + bounds.lengthY() - (node.y + yOffset);
                    if (requiredJumpHeight > jumpHeight) {
                        return;
                    }

                    if (bounds.lengthY() > tallestSolid.bounds().lengthY()) {
                        tallestSolid = solid;
                    }
                }
            }

            boolean tallestEmpty = tallestSolid.isEmpty();
            Bounds3D tallestBounds = tallestSolid.bounds();
            if (!tallestEmpty) {
                double highestLayerY = y + tallestBounds.lengthY();
                if (highestLayerY > exactY) {
                    exactY = highestLayerY;
                }
            }

            //found somewhere we can fit, we can stop checking layers now
            if ((tallestEmpty ? y + 1 : tallestBounds.originY() - exactY) >= height) {
                newY = exactY;
                break;
            }
        }

        //newY was never assigned, so we can't move this direction
        if (Double.isNaN(newY)) {
            return;
        }

        //jumping is necessary, so we need to check above us
        if (newY > node.y + yOffset) {
            //only search as high as we need to in order to reach the target elevation
            int jumpSearch = (int)Math.ceil(newY - (node.y + yOffset));

            //check for blocks above the agent
            for (int i = fullHeight ? 0 : -1; i < jumpSearch; i++) {
                int y = i + ceilHeight + node.y;

                for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                    for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                        int x = node.x + dex;
                        int z = node.z + dez;

                        Solid solid = space.solidAt(x, y, z);

                        //no collision with empty solids
                        if (solid.isEmpty()) {
                            continue;
                        }

                        //simpler check for full solids
                        if (solid.isFull()) {
                            //return if this solid prevents us from jumping high enough
                            if (y - height < newY) {
                                return;
                            }

                            continue;
                        }

                        Bounds3D bounds = solid.bounds();
                        double originY = bounds.originY();

                        //this solid is too high to worry about colliding with
                        if (originY - height >= newY) {
                            continue;
                        }

                        if (overlaps(x, z, bounds, node)) {
                            return;
                        }
                    }
                }
            }

            //nothing was found preventing our jump
            int blockY = (int)Math.floor(newY);
            handler.handle(node, nx, blockY, nz, newY - blockY);
            return;
        }

        //search below us
        for (int i = 0; i < fallSearchHeight; i++) {
            int y = node.y - (i + 1);

            double highestY = Double.NEGATIVE_INFINITY;
            for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                    int x = node.x + dex;
                    int z = node.z + dez;

                    Solid solid = space.solidAt(x, y, z);
                    if (solid.isEmpty()) {
                        continue;
                    }

                    //we automatically know this is the highest solid
                    if (solid.isFull()) {
                        highestY = 1;
                        break;
                    }

                    Bounds3D bounds = solid.bounds();
                    if (overlaps(x, z, bounds, node)) {
                        double height = bounds.lengthY();
                        if (height > highestY) {
                            highestY = height;
                        }
                    }
                }
            }

            //finite if we found a block
            if (Double.isFinite(highestY)) {
                int blockY = (int)Math.floor(newY);
                handler.handle(node, nx, blockY, nz, newY - blockY);
                return;
            }
        }
    }

    private boolean expandOverlaps(int x, int z, double dx, double dz, Bounds3D bounds, Node node) {
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

        return overlaps(box, boz, bounds.lengthX(), bounds.lengthZ(), aox, aoz, awx, awz);
    }

    //two-dimensional collision check that only takes into account the xz plane
    private boolean overlaps(int x, int z, Bounds3D bounds, Node node) {
        double box = x + bounds.originX();
        double boz = z + bounds.originZ();

        double aox = node.x + 0.5D - halfWidth;
        double aoz = node.z + 0.5D - halfWidth;

        return overlaps(box, boz, bounds.lengthX(), bounds.lengthZ(), aox, aoz, width, width);
    }

    private static boolean overlaps(double ox1, double oz1, double lx1, double lz1,
            double ox2, double oz2, double lx2, double lz2) {
        double mx1 = ox1 + lx1;
        double mz1 = oz1 + lz1;

        double mx2 = ox2 + lx2;
        double mz2 = oz2 + lz2;

        return mx1 > ox2 && mz1 > oz2 && mx2 > ox1 && mz2 > ox1;
    }

    @Override
    public boolean canSkip(@NotNull Node currentNode, @NotNull Node parent, @NotNull Direction direction) {
        int dX = parent.x - currentNode.x;
        int dZ = parent.z - currentNode.z;

        return Math.signum(dX) == direction.x && Math.signum(dZ) == direction.z;
    }
}
