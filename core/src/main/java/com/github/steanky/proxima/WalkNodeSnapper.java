package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.solid.Solid;
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

        this.fullWidth = width == Math.rint(width);
        this.fullHeight = height == Math.rint(height);

        this.width = Math.max(width - epsilon, 0);
        this.halfWidth = width / 2;
        this.wDiff = (Math.ceil(width) - width) / 2;
        this.height = Math.max(height - epsilon, 0);
        this.ceilHeight = (int)Math.ceil(height);

        //silly, totally unnecessary way to add 1 to a number only if it is even
        int blockWidth = ((int)Math.ceil(width)) | 1;

        this.searchHeight = (int)Math.ceil(height + jumpHeight);
        this.fallSearchHeight = (int)Math.ceil(fallTolerance);

        this.halfBlockWidth = blockWidth >> 1;

        this.jumpHeight = jumpHeight;
        this.space = Objects.requireNonNull(space);
        this.searchArea = searchArea.immutable();
    }

    private static void check(double width, double height, double fallTolerance, double jumpHeight, double epsilon) {
        //width must be non-negative and finite
        if (width < 0 || !Double.isFinite(width)) {
            throw new IllegalArgumentException("Invalid width: " + width);
        }

        //height must be non-negative and finite
        if (height < 0 || !Double.isFinite(height)) {
            throw new IllegalArgumentException("Invalid height: " + height);
        }

        //fallTolerance must be non-negative and not a NaN (positive infinity is allowed)
        if (fallTolerance < 0 || Double.isNaN(fallTolerance)) {
            throw new IllegalArgumentException("Invalid fallTolerance: " + fallTolerance);
        }

        //jumpHeight must be non-negative and not a NaN (positive infinity is allowed)
        if (jumpHeight < 0 || Double.isNaN(jumpHeight)) {
            throw new IllegalArgumentException("Invalid jumpHeight: " + jumpHeight);
        }

        //epsilon must be positive and finite
        if (epsilon < 0 || !Double.isFinite(epsilon)) {
            throw new IllegalArgumentException("Invalid epsilon: " + epsilon);
        }
    }

    @Override
    public void snap(@NotNull Direction direction, @NotNull Node node, @NotNull NodeHandler handler) {
        int dx = direction.x;
        int dz = direction.z;

        int nx = node.x + dx;
        int nz = node.z + dz;

        //fast exit: don't check out of bounds
        if (searchArea.originX() > nx || searchArea.originZ() > nz ||
                searchArea.maxX() <= nx || searchArea.maxZ() <= nz) {
            return;
        }

        double yOffset = node.yOffset;
        double exactY = node.y + yOffset;
        double newY = Double.NaN;

        for (int i = 0; i < searchHeight; i++) {
            int y = node.y + i;

            Bounds3D tallestBounds = null;

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
                        //agent coordinates relative to solid and shifted over by wDiff * width
                        double ax = (((node.x + 0.5) - x) - halfWidth) + (dx > 0 ? width : wDiff * dx);
                        double ay = (node.y + yOffset) - y;
                        double az = (((node.z + 0.5) - z) - halfWidth) + (dz > 0 ? width : wDiff * dz);

                        double lx = dx == 0 ? width : wDiff;
                        double lz = dz == 0 ? width : wDiff;

                        Bounds3D bounds;
                        if ((bounds = solid.overlaps(ax, ay, az, lx, height, lz, Solid.Order.HIGHEST)) != null) {
                            //fast exit: can't jump over this block
                            if (y + bounds.lengthY() - node.y > jumpHeight) {
                                return;
                            }

                            if (tallestBounds == null || bounds.lengthY() > tallestBounds.lengthY()) {
                                tallestBounds = bounds;
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
                    double requiredJumpHeight = y + 1 - exactY;
                    if (requiredJumpHeight > jumpHeight) {
                        return;
                    }

                    tallestBounds = solid.bounds();
                    continue;
                }

                //agent coordinates relative to solid
                double ax = ((node.x + 0.5) - x) - halfWidth;
                double ay = (node.y + yOffset) - y;
                double az = ((node.z + 0.5) - z) - halfWidth;

                //if the directionally-expanded bounds overlaps, we have a collision
                Bounds3D bounds;
                if ((bounds = solid.expandOverlaps(ax, ay, az, width, height, width, dx, 0, dz,
                        Solid.Order.HIGHEST)) != null) {
                    double requiredJumpHeight = y + bounds.lengthY() - exactY;
                    if (requiredJumpHeight > jumpHeight) {
                        return;
                    }

                    if (tallestBounds == null || bounds.lengthY() > tallestBounds.lengthY()) {
                        tallestBounds = bounds;
                    }
                }
            }

            boolean tallestEmpty = tallestBounds == null;
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
        //or, our target y is outside the search area
        if (Double.isNaN(newY) || newY < searchArea.originY() || newY >= searchArea.maxY()) {
            return;
        }

        //jumping is necessary, so we need to check above us
        if (newY > node.y + yOffset) {
            //only search as high as we need to in order to reach the target elevation
            int jumpSearch = (int)Math.ceil(newY - exactY);

            //check for blocks above the agent, possibly including the block intersected by the agent's head
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
                            //if full height: any block we run into makes this jump impossible
                            if (fullHeight) {
                                return;
                            }

                            //return if this solid prevents us from jumping high enough, and we aren't intersecting
                            if (y - height < newY && y > node.y + yOffset + height) {
                                return;
                            }

                            continue;
                        }

                        Bounds3D bounds = solid.bounds();

                        //this solid is too high to worry about colliding with
                        if (bounds.originY() - height >= newY) {
                            continue;
                        }

                        //agent coordinates relative to solid
                        double ax = ((node.x + 0.5) - x) - halfWidth;
                        double ay = ((node.y + yOffset) - y) + height;
                        double az = ((node.z + 0.5) - z) - halfWidth;

                        if (solid.overlaps(ax, ay, az, width, jumpHeight, width, Solid.Order.LOWEST) != null) {
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

        //search below us, possibly including the block we're in if it's partial
        for (int i = node.yOffset == 0 ? 0 : -1; i < fallSearchHeight; i++) {
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

                    //agent coordinates relative to solid
                    double ax = ((node.x + 0.5) - x) - halfWidth;
                    double ay = (node.y + yOffset) - y;
                    double az = ((node.z + 0.5) - z) - halfWidth;

                    Bounds3D bounds;
                    if ((bounds = solid.overlaps(ax, ay, az, width, height, width, Solid.Order.HIGHEST)) != null) {
                        double height = bounds.lengthY();

                        if (height > highestY) {
                            highestY = height;
                        }
                    }
                }
            }

            //finite if we found a block
            if (Double.isFinite(highestY)) {
                double ny = y + highestY;
                int blockY = (int)Math.floor(ny);
                handler.handle(node, nx, blockY, nz, ny - blockY);
                return;
            }
        }
    }

    @Override
    public boolean canSkip(@NotNull Node currentNode, @NotNull Node parent, @NotNull Direction direction) {
        int dX = parent.x - currentNode.x;
        int dZ = parent.z - currentNode.z;

        return Math.signum(dX) == direction.x && Math.signum(dZ) == direction.z;
    }
}
