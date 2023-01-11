package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BasicNodeSnapper implements NodeSnapper {
    private final double halfWidth;
    private final double fallTolerance;
    private final double height;

    private final int searchHeight;
    private final int fallSearchHeight;

    private final boolean fullWidth;

    private final int halfBlockWidth;

    private final double jumpHeight;
    private final Space space;
    private final boolean walk;

    private final double adjustedWidth;
    private final double adjustedHeight;

    public BasicNodeSnapper(double width, double height, double fallTolerance, double jumpHeight,
            @NotNull Space space, boolean walk, double epsilon) {
        validate(width, height, fallTolerance, jumpHeight, epsilon);

        if (!walk) {
            jumpHeight = 0;
            fallTolerance = 0;
        }

        int rWidth = (int) Math.rint(width);

        this.fullWidth = width == rWidth && (rWidth & 1) != 0;
        this.fallTolerance = fallTolerance;

        //silly, totally unnecessary way to add 1 to a number only if it is even
        int blockWidth = ((int) Math.ceil(width)) | 1;

        this.halfWidth = (width / 2) - (epsilon / 2);
        this.height = height;

        this.searchHeight = (int) Math.ceil(height + jumpHeight);
        this.fallSearchHeight = (int) Math.ceil(fallTolerance) + 1;

        this.halfBlockWidth = blockWidth / 2;
        this.jumpHeight = jumpHeight;
        this.space = Objects.requireNonNull(space);
        this.walk = walk;

        this.adjustedWidth = width - epsilon;
        this.adjustedHeight = height - epsilon;
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

    private long snapVertical(Direction direction, int nodeX, int nodeY, int nodeZ, float nodeOffset) {
        int dx = direction.x;
        int dz = direction.z;

        int nx = nodeX + dx;
        int nz = nodeZ + dz;

        double exactY = nodeY + nodeOffset;

        double ax = nodeX + 0.5 - halfWidth;
        double az = nodeZ + 0.5 - halfWidth;

        if (direction == Direction.UP) {
            double exactHeight = height + nodeOffset;
            int ceilHeight = (int)Math.ceil(exactHeight);
            int offset = ceilHeight + nodeY;
            boolean fullHeight = exactHeight == ceilHeight;

            for (int i = fullHeight ? 0 : -1; i < 1; i++) {
                int y = i + offset;

                for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                    for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                        int x = nx + dex;
                        int z = nz + dez;

                        Solid solid = space.solidAt(x, y, z);
                        if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                            continue;
                        }

                        if (solid.hasCollision(x, y, z, ax, exactY, az, adjustedWidth, adjustedHeight, adjustedWidth,
                                Direction.UP, 1)) {
                            return FAIL;
                        }
                    }
                }
            }

            return NodeSnapper.encode(nodeY + 1);
        }

        for (int i = nodeOffset == 0 ? 0 : -1; i < 1; i++) {
            int y = nodeY - (i + 1);

            for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                    int x = nx + dex;
                    int z = nz + dez;

                    Solid solid = space.solidAt(x, y, z);
                    if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                        continue;
                    }

                    if (solid.hasCollision(x, y, z, ax, exactY, az, adjustedWidth, adjustedHeight, adjustedWidth,
                            Direction.DOWN, 1)) {
                        return FAIL;
                    }
                }
            }
        }

        return NodeSnapper.encode(nodeY - 1);
    }

    @Override
    public long snap(@NotNull Direction direction, int nodeX, int nodeY, int nodeZ, float nodeOffset) {
        if (direction.ordinal() > 3) {
            //can't pathfind straight up or down while walking
            if (walk) {
                return FAIL;
            }

            //if not walking, we can do vertical snaps
            return snapVertical(direction, nodeX, nodeY, nodeZ, nodeOffset);
        }

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

        double ax = nodeX + 0.5 - halfWidth;
        double az = nodeZ + 0.5 - halfWidth;

        for (int i = 0; i < actualSearchHeight; i++) {
            int y = nodeY + i;

            float lowest = Float.POSITIVE_INFINITY;
            float highest = Float.NEGATIVE_INFINITY;

            //search an individual y-layer of blocks
            for (int dh = -halfBlockWidth; dh <= halfBlockWidth; dh++) {
                //executes either once or twice, depending on if we're full-width
                for (int j = fullWidth ? 1 : 0; j < 2; j++) {
                    int x = dx == 0 ? nx + dh : nodeX + (dx * halfBlockWidth) + (dx * j);
                    int z = dz == 0 ? nz + dh : nodeZ + (dz * halfBlockWidth) + (dz * j);

                    Solid solid = space.solidAt(x, y, z);

                    if (solid.isEmpty() || (j == 0 && solid.isFull())) {
                        continue;
                    }

                    if (solid.isFull()) {
                        highest = 1;
                        lowest = 0;

                        //we know the tallest bounds here is this solid
                        break;
                    }

                    long res = solid.minMaxCollision(x, y, z, ax, exactY, az, adjustedWidth,
                            adjustedHeight + jumpHeight, adjustedWidth, direction, 1);
                    float low = Solid.lowest(res);
                    float high = Solid.highest(res);

                    if (low < lowest) {
                        lowest = low;
                    }

                    if (high > highest) {
                        highest = high;
                    }

                    if (low == 0 && high == 1) {
                        break;
                    }
                }
            }

            //if we found a solid this layer, check the gap below it
            if (Float.isFinite(highest)) {
                double ceiling = y + lowest;

                if (ceiling - lastTargetY > adjustedHeight) {
                    newY = lastTargetY;
                    break;
                }

                lastTargetY = y + highest;

                //too high to make this jump
                if (lastTargetY - exactY > jumpHeight) {
                    return FAIL;
                }
            } else if ((y + 1) - lastTargetY > adjustedHeight) {
                newY = lastTargetY;
                break;
            }
        }

        //newY was never assigned, so we can't move this direction
        if (Double.isNaN(newY)) {
            return FAIL;
        }

        //jumping is necessary, so we need to check above us
        if (newY > exactY) {
            //only search as high as we need to in order to reach the target elevation
            int jumpSearch = (int) Math.ceil(newY - exactY);

            double exactHeight = height + nodeOffset;
            int ceilHeight = (int)Math.ceil(exactHeight);
            int offset = ceilHeight + nodeY;

            boolean fullHeight = exactHeight == ceilHeight;

            //check for blocks above the agent, possibly including the block intersected by the agent's head
            for (int i = fullHeight ? 0 : -1; i < jumpSearch; i++) {
                int y = i + offset;

                for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                    for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                        int x = nodeX + dex;
                        int z = nodeZ + dez;

                        Solid solid = space.solidAt(x, y, z);

                        //no collision with empty solids
                        if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                            continue;
                        }

                        //any full solids encountered will block our jump
                        if (solid.isFull()) {
                            return FAIL;
                        }

                        Bounds3D closest = solid.closestCollision(x, y, z, ax, exactY, az, adjustedWidth,
                                adjustedHeight, adjustedWidth, Direction.UP, jumpHeight);

                        if (closest != null && y + closest.originY() - newY < height) {
                            return FAIL;
                        }
                    }
                }
            }

            //nothing was found preventing our jump
            return NodeSnapper.encode(newY);
        }

        //search below us, possibly including the block we're in if it's partial
        for (int i = nodeOffset == 0 ? 0 : -1; i < fallSearchHeight; i++) {
            int y = nodeY - (i + 1);

            double highestY = Double.NEGATIVE_INFINITY;

            double nax = ax + dx;
            double naz = az + dz;

            outer:
            for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                    int x = nx + dex;
                    int z = nz + dez;

                    Solid solid = space.solidAt(x, y, z);
                    if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                        continue;
                    }

                    //we automatically know this is the highest solid
                    if (solid.isFull()) {
                        highestY = 1;
                        break outer;
                    }

                    Bounds3D bounds = solid.closestCollision(x, y, z, nax, exactY, naz, adjustedWidth, adjustedHeight,
                            adjustedWidth, Direction.DOWN, fallSearchHeight);

                    if (bounds != null) {
                        double height = bounds.maxY();
                        if (height > highestY) {
                            highestY = height;
                            if (height == 1) {
                                break outer;
                            }
                        }
                    }
                }
            }

            //only search 1 layer if flying, just so we adjust our offset if necessary
            if (!walk) {
                return NodeSnapper.encode(y + (Double.isFinite(highestY) ? highestY : 0));
            }

            //finite if we found a block
            if (Double.isFinite(highestY)) {
                double ty = y + highestY;
                double fall = exactY - ty;

                if (fall <= fallTolerance) {
                    return NodeSnapper.encode(ty);
                }
            }
        }

        return FAIL;
    }

    @Override
    public float checkInitial(double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;

        double aox = x - halfWidth;
        double aoy = y;
        double aoz = z - halfWidth;

        double alx = adjustedWidth;
        double aly = adjustedHeight;
        double alz = adjustedWidth;

        double amx = aox + alx;
        double amy = aoy + aly;
        double amz = aoz + alz;

        boolean cx = dx != 0;
        boolean cy = dy != 0;
        boolean cz = dz != 0;

        int sx = (int) Math.floor(aox + Math.min(0, dx));
        int ex = (int) Math.floor(amx + Math.abs(dx));

        int sy = (int) Math.floor(aoy + Math.min(0, dy));
        int ey = (int) Math.floor(amy + Math.abs(dy));

        int sz = (int) Math.floor(aoz + Math.min(0, dz));
        int ez = (int) Math.floor(amz + Math.abs(dz));

        if (cx) {
            boolean full = dx < 0 ? x == Math.rint(x) : amx == Math.rint(amx);
            int o = dx < 0 ? (int)Math.floor(x) : (int)Math.floor(amx);
            int sdx = (int)Math.signum(dx);

            for (int i = full ? 1 : 0; i < (sx == ex ? 1 : 2); i++) {
                int bx = o + i * sdx;

                for (int by = sy; by <= ey; by++) {
                    for (int bz = sz; bz <= ez; bz++) {
                        Solid solid = space.solidAt(bx, by, bz);

                        if (solid.isEmpty() || i == 0 && solid.isFull()) {
                            continue;
                        }

                        if (solid.hasCollision(bx, by, bz, aox, aoy, aoz, alx, aly, alz, dx, dy, dz)) {
                            return Float.NaN;
                        }
                    }
                }
            }
        }

        if (cz) {
            boolean full = dz < 0 ? z == Math.rint(z) : amz == Math.rint(amz);
            int o = dz < 0 ? (int)Math.floor(z) : (int)Math.floor(amz);
            int sdz = (int)Math.signum(dz);

            boolean limitMinX;
            boolean limitMaxX;

            if (cx) {
                if (dx < 0) {
                    limitMinX = true;
                    limitMaxX = false;
                }
                else {
                    limitMinX = false;
                    limitMaxX = true;
                }
            }
            else {
                limitMinX = false;
                limitMaxX = false;
            }

            for (int i = full ? 1 : 0; i < (sz == ez ? 1 : 2); i++) {
                int bz = o + i * sdz;

                for (int by = sy; by <= ey; by++) {
                    for (int bx = limitMinX ? sx + 1 : sx; bx <= (limitMaxX ? ex - 1 : ex); bx++) {
                        Solid solid = space.solidAt(bx, by, bz);

                        if (solid.isEmpty() || i == 0 && solid.isFull()) {
                            continue;
                        }

                        if (solid.hasCollision(bx, by, bz, aox, aoy, aoz, alx, aly, alz, dx, dy, dz)) {
                            return Float.NaN;
                        }
                    }
                }
            }
        }

        if (cy) {
            boolean full = dy < 0 ? y == Math.rint(y) : amy == Math.rint(amy);
            int o = dy < 0 ? (int)Math.floor(y) : (int)Math.floor(amy);
            int sdy = (int)Math.signum(dy);

            boolean limitMinX;
            boolean limitMaxX;

            if (cx) {
                if (dx < 0) {
                    limitMinX = true;
                    limitMaxX = false;
                }
                else {
                    limitMinX = false;
                    limitMaxX = true;
                }
            }
            else {
                limitMinX = false;
                limitMaxX = false;
            }

            boolean limitMinZ;
            boolean limitMaxZ;

            if (cz) {
                if (dz < 0) {
                    limitMinZ = true;
                    limitMaxZ = false;
                }
                else {
                    limitMinZ = false;
                    limitMaxZ = true;
                }
            }
            else {
                limitMinZ = false;
                limitMaxZ = false;
            }

            for (int i = full ? 1 : 0; i < (sy == ey ? 1 : 2); i++) {
                int by = o + i * sdy;

                for (int bx = limitMinX ? sx + 1 : sx; bx <= (limitMaxX ? ex - 1 : ex); bx++) {
                    for (int bz = limitMinZ ? sz + 1 : sz; bz <= (limitMaxZ ? ez - 1 : ez); bz++) {
                        Solid solid = space.solidAt(bx, by, bz);

                        if (solid.isEmpty() || i == 0 && solid.isFull()) {
                            continue;
                        }

                        if (solid.hasCollision(bx, by, bz, aox, aoy, aoz, alx, aly, alz, dx, dy, dz)) {
                            return Float.NaN;
                        }
                    }
                }
            }
        }

        Solid solid = space.solidAt((int) Math.floor(tx), (int) Math.floor(ty), (int) Math.floor(tz));
        if (solid.isEmpty() || solid.isFull()) {
            return 0;
        }
        else {
            return (float) solid.bounds().maxY();
        }
    }
}