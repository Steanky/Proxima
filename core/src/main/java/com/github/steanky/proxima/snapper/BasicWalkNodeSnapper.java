package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class BasicWalkNodeSnapper implements WalkNodeSnapper {
    private final double width;
    private final double halfWidth;
    private final double fallTolerance;
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

    public BasicWalkNodeSnapper(double width, double height, double fallTolerance, double jumpHeight,
            @NotNull Space space, double epsilon) {
        validate(width, height, fallTolerance, jumpHeight, epsilon);

        int rWidth = (int) Math.rint(width);

        this.fullWidth = width == rWidth && (rWidth & 1) != 0;
        this.fullHeight = height == Math.rint(height);
        this.fallTolerance = fallTolerance + epsilon;

        //silly, totally unnecessary way to add 1 to a number only if it is even
        int blockWidth = ((int) Math.ceil(width)) | 1;

        this.width = width - epsilon;
        this.halfWidth = width / 2;
        this.wDiff = ((blockWidth - width) / 2) - (epsilon / 2);
        this.height = height - epsilon;
        this.ceilHeight = (int) Math.ceil(height);

        this.searchHeight = (int) Math.ceil(height + jumpHeight);
        this.fallSearchHeight = (int) Math.ceil(fallTolerance) + 1;

        this.halfBlockWidth = blockWidth >> 1;

        this.jumpHeight = jumpHeight + epsilon;
        this.space = Objects.requireNonNull(space);
    }

    private static void validate(double width, double height, double fallTolerance, double jumpHeight, double epsilon) {
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

        //epsilon can't reduce width to 0 or lower
        if (epsilon >= width) {
            throw new IllegalArgumentException("Epsilon must not be larger than or equal to width");
        }

        //epsilon can't reduce height to 0 or lower
        if (epsilon >= height) {
            throw new IllegalArgumentException("Epsilon must not be larger than or equal to height");
        }
    }

    @SuppressWarnings({"DuplicatedCode", "ForLoopReplaceableByForEach"})
    @Override
    public long snap(@NotNull Direction direction, int nodeX, int nodeY, int nodeZ, double nodeOffset) {
        int dx = direction.x;
        int dz = direction.z;

        int nx = nodeX + dx;
        int nz = nodeZ + dz;

        double exactY = nodeY + nodeOffset;
        double newY = Double.NaN;
        double lastTargetY = exactY;

        //the actual number of blocks we need to check vertically may vary depending on the nodeOffset
        int actualSearchHeight = nodeOffset == 0 ? searchHeight :
                ((int)Math.ceil(nodeOffset + height + jumpHeight) - nodeY) + 1;

        for (int i = 0; i < actualSearchHeight; i++) {
            int y = nodeY + i;

            Bounds3D tallest = null;
            Bounds3D lowest = null;

            //search an individual layer
            outer:
            for (int dh = -halfBlockWidth; dh <= halfBlockWidth; dh++) {
                //full agents don't need to check for collisions inside themselves
                if (!fullWidth) {
                    //first block we may encounter
                    int x = dx == 0 ? nx + dh : nodeX + (dx * halfBlockWidth);
                    int z = dz == 0 ? nz + dh : nodeZ + (dz * halfBlockWidth);

                    Solid solid = space.solidAt(x, y, z);

                    //if the solid is full, we know we're overlapping it (therefore no collision)
                    //if the solid is empty, it has no collision
                    //if the solid is partial, check if we're overlapping (we may have collision)
                    if (!solid.isEmpty() && !solid.isFull()) {
                        //agent coordinates relative to solid and shifted over by wDiff * width
                        //this is the area within the same block as the agent, that it will travel through in order to
                        //move to the next node, that does not include its current occupied space
                        double ax = (((nodeX + 0.5) - x) - halfWidth) + (dx > 0 ? width : wDiff * dx);
                        double ay = exactY - y;
                        double az = (((nodeZ + 0.5) - z) - halfWidth) + (dz > 0 ? width : wDiff * dz);

                        double lx = dx == 0 ? width : wDiff;
                        double lz = dz == 0 ? width : wDiff;

                        List<Bounds3D> children = solid.children();
                        for (int j = 0; j < children.size(); j++) {
                            Bounds3D child = children.get(j);

                            if (!child.overlaps(ax, ay, az, lx, height, lz)) {
                                //no overlap means this block won't impede our movement
                                continue;
                            }

                            double cmy = child.maxY();
                            double coy = child.originY();
                            if (tallest == null || cmy > tallest.maxY()) {
                                tallest = child;
                            }

                            if (lowest == null || coy < lowest.originY()) {
                                lowest = child;
                            }

                            //highest and lowest possible was found for this layer
                            if (cmy == 1 && coy == 0) {
                                break outer;
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
                    tallest = solid.bounds();
                    lowest = tallest;

                    //we know the tallest bounds here is this solid
                    break;
                }

                //agent coordinates relative to solid
                double ax = ((nodeX + 0.5) - x) - halfWidth;
                double ay = exactY - y;
                double az = ((nodeZ + 0.5) - z) - halfWidth;

                double lx = width + Math.abs(dx);
                double lz = width + Math.abs(dz);

                if (dx < 0) {
                    ax += dx;
                }

                if (dz < 0) {
                    az += dz;
                }

                //if the directionally-expanded bounds overlaps, we have a collision
                List<Bounds3D> children = solid.children();
                for (int j = 0; j < children.size(); j++) {
                    Bounds3D child = children.get(j);

                    if (!child.overlaps(ax, ay, az, lx, height, lz)) {
                        continue;
                    }

                    double cmy = child.maxY();
                    double coy = child.originY();
                    if (tallest == null || cmy > tallest.maxY()) {
                        tallest = child;
                    }

                    if (lowest == null || coy < lowest.originY()) {
                        lowest = child;
                    }

                    if (cmy == 1 && coy == 0) {
                        break outer;
                    }
                }
            }

            //if we found a solid this layer, check the gap below it
            if (tallest != null) {
                double ceiling = y + lowest.originY();

                if (ceiling - lastTargetY >= height) {
                    newY = lastTargetY;
                    break;
                }

                lastTargetY = y + tallest.maxY();

                //too high to make this jump
                if (lastTargetY - exactY > jumpHeight) {
                    return FAIL;
                }
            } else if ((y + 1) - lastTargetY >= height) {
                newY = lastTargetY;
                break;
            }
        }

        //newY was never assigned, so we can't move this direction
        if (Double.isNaN(newY)) {
            return FAIL;
        }

        //jumping is necessary, so we need to check above us
        if (newY > exactY && jumpHeight != 0) {
            //only search as high as we need to in order to reach the target elevation
            int jumpSearch = (int) Math.ceil(newY - exactY);

            //check for blocks above the agent, possibly including the block intersected by the agent's head
            for (int i = fullHeight ? 0 : -1; i < jumpSearch; i++) {
                int y = i + ceilHeight + nodeY;

                for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                    for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                        int x = nodeX + dex;
                        int z = nodeZ + dez;

                        Solid solid = space.solidAt(x, y, z);

                        //no collision with empty solids
                        if (solid.isEmpty()) {
                            continue;
                        }

                        //simpler check for full solids
                        if (solid.isFull()) {
                            //if full height: any block we run into makes this jump impossible
                            if (fullHeight) {
                                return FAIL;
                            }

                            //return if this solid prevents us from jumping high enough, and we aren't intersecting
                            if (y - height < newY && y > exactY + height) {
                                return FAIL;
                            }

                            continue;
                        }

                        Bounds3D bounds = solid.bounds();

                        //this solid is too high to worry about colliding with
                        if (bounds.originY() - height >= newY) {
                            continue;
                        }

                        //agent coordinates relative to solid
                        double ax = ((nodeX + 0.5) - x) - halfWidth;
                        double ay = (exactY - y) + height;
                        double az = ((nodeZ + 0.5) - z) - halfWidth;

                        List<Bounds3D> children = solid.children();
                        for (int j = 0; j < children.size(); j++) {
                            Bounds3D child = children.get(j);
                            if (child.overlaps(ax, ay, az, width, jumpHeight, width) &&
                                    y + child.originY() - height < newY) {
                                return FAIL;
                            }
                        }
                    }
                }
            }

            //nothing was found preventing our jump
            return WalkNodeSnapper.encode(newY);
        }

        //search below us, possibly including the block we're in if it's partial
        for (int i = nodeOffset == 0 ? 0 : -1; i < fallSearchHeight; i++) {
            int y = nodeY - (i + 1);

            double highestY = Double.NEGATIVE_INFINITY;

            outer:
            for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                    int x = nx + dex;
                    int z = nz + dez;

                    Solid solid = space.solidAt(x, y, z);
                    if (solid.isEmpty()) {
                        continue;
                    }

                    //we automatically know this is the highest solid
                    if (solid.isFull()) {
                        highestY = 1;
                        break outer;
                    }

                    //agent coordinates relative to solid
                    double ax = ((nx + 0.5) - x) - halfWidth;
                    double az = ((nz + 0.5) - z) - halfWidth;

                    List<Bounds3D> children = solid.children();
                    for (int j = 0; j < children.size(); j++) {
                        Bounds3D child = children.get(j);

                        if (!child.overlaps(ax, 0, az, width, height, width)) {
                            continue;
                        }

                        double height = child.maxY();
                        if (height > highestY) {
                            highestY = height;
                        }

                        if (height == 1) {
                            break outer;
                        }
                    }
                }
            }

            //finite if we found a block
            if (Double.isFinite(highestY)) {
                double ty = y + highestY;
                double fall = exactY - ty;

                if (fall <= fallTolerance) {
                    return WalkNodeSnapper.encode(ty);
                }
            }
        }

        return FAIL;
    }
}