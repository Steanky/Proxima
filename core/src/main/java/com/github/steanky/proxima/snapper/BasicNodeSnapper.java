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
                        int x = nodeX + dex;
                        int z = nodeZ + dez;

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

            return NodeSnapper.encode(nodeY + 1, false, 0);
        }

        for (int i = nodeOffset == 0 ? 0 : -1; i < 1; i++) {
            int y = nodeY - (i + 1);

            for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                    int x = nodeX + dex;
                    int z = nodeZ + dez;

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

        return NodeSnapper.encode(nodeY - 1, false, 0);
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
        int actualSearchHeight = computeJumpSearchHeight(nodeY, exactY);

        double ax = nodeX + 0.5 - halfWidth;
        double az = nodeZ + 0.5 - halfWidth;

        //we may need to jump over a block to get to the next node
        boolean highestIsIntermediate = false;

        for (int i = 0; i < actualSearchHeight; i++) {
            int y = nodeY + i;

            float lowest = Float.POSITIVE_INFINITY;
            float highest = Float.NEGATIVE_INFINITY;

            //search an individual y-layer of blocks
            outer:
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

                        highestIsIntermediate = false;

                        //we know the tallest bounds here is this solid
                        break outer;
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

                        //update flag to keep track of what we're actually jumping over
                        highestIsIntermediate = j == 0;
                    }

                    if (low == 0 && high == 1) {
                        break outer;
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
            int obx = nodeX - halfBlockWidth;
            int mbx = nodeX + halfBlockWidth;

            int obz = nodeZ - halfBlockWidth;
            int mbz = nodeZ + halfBlockWidth;

            if (checkJump(obx, mbx, obz, mbz, ax, exactY, az, newY)) {
                return FAIL;
            }

            if (!highestIsIntermediate) {
                //if non-intermediate, we don't have to check below us to determine our actual height
                return NodeSnapper.encode(newY, false, 0);
            }
        }

        int oby = (int)Math.floor(newY);
        boolean full = newY == oby;

        if (full && !walk) {
            return NodeSnapper.encode(newY, false, 0);
        }

        int obx = nx - halfBlockWidth;
        int mbx = nx + halfBlockWidth;

        int obz = nz - halfBlockWidth;
        int mbz = nz + halfBlockWidth;

        double nax = ax + dx;
        double naz = az + dz;

        double finalY = checkFall(obx, mbx, obz, mbz, oby, nax, newY, naz);
        if (Double.isNaN(finalY)) {
            return FAIL;
        }

        boolean intermediate = finalY != newY && highestIsIntermediate;
        return NodeSnapper.encode(finalY, intermediate, intermediate ? (float) (newY - finalY) : 0F);
    }

    @Override
    public float checkInitial(double x, double y, double z, double tx, double ty, double tz) {
        double aox = x - halfWidth;
        double aoz = z - halfWidth;

        double amx = aox + adjustedWidth;
        double amz = aoz + adjustedWidth;

        int obx = (int)Math.floor(aox);
        int oby = (int)Math.floor(y);
        int obz = (int)Math.floor(aoz);

        int mbx = (int)Math.floor(amx);
        int mbz = (int)Math.floor(amz);

        double adjustedY = checkFall(obx, mbx, obz, mbz, oby, aox, y, aoz);
        if (Double.isNaN(adjustedY)) {
            //invalid start location
            return Float.NaN;
        }

        double dx = tx - x;
        double dz = tz - z;

        boolean cx = dx != 0;
        boolean cz = dz != 0;

        int sx = (int) Math.floor(aox + Math.min(0, dx));
        int ex = (int) Math.floor(amx + Math.abs(dx));

        int sz = (int) Math.floor(aoz + Math.min(0, dz));
        int ez = (int) Math.floor(amz + Math.abs(dz));

        int adjustedBlockY = (int)Math.floor(adjustedY);

        int actualSearchHeight = computeJumpSearchHeight(adjustedBlockY, adjustedY);

        boolean limitMinX = cx && dx < 0;
        boolean limitMaxX = cx && dx > 0;

        boolean xf = isFull(dx, x, amx);
        int xo = computeOffset(dx, x, amx);
        int sdx = (int)Math.signum(dx);

        boolean zf = isFull(dz, z, amz);
        int zo = computeOffset(dz, z, amz);
        int sdz = (int)Math.signum(dz);

        double newY = Double.NaN;
        double lastTargetY = adjustedY;

        boolean highestIsIntermediate = false;

        for (int i = 0; i < actualSearchHeight; i++) {
            int by = adjustedBlockY + i;

            float lowest = Float.POSITIVE_INFINITY;
            float highest = Float.NEGATIVE_INFINITY;

            if (cx) {
                outer:
                for (int j = xf ? 1 : 0; j < (sx == ex ? 1 : 2); j++) {
                    int bx = xo + j * sdx;

                    for (int bz = sz; bz <= ez; bz++) {
                        long res = diagonalMinMax(bx, by, bz, j, aox, adjustedY, aoz, adjustedWidth, adjustedHeight,
                                adjustedWidth, dx, dz);
                        float low = Solid.lowest(res);
                        float high = Solid.highest(res);

                        if (low < lowest) {
                            lowest = low;
                        }

                        if (high > highest) {
                            highest = high;
                            highestIsIntermediate = j == 0;
                        }

                        if (low == 0 && high == 1) {
                            break outer;
                        }
                    }
                }
            }

            if (cz) {
                outer:
                for (int j = zf ? 1 : 0; j < (sz == ez ? 1 : 2); j++) {
                    int bz = zo + j * sdz;

                    for (int bx = limitMinX ? sx + 1 : sx; bx <= (limitMaxX ? ex - 1 : ex); bx++) {
                        long res = diagonalMinMax(bx, by, bz, j, aox, adjustedY, aoz, adjustedWidth, adjustedHeight,
                                adjustedWidth, dx, dz);
                        float low = Solid.lowest(res);
                        float high = Solid.highest(res);

                        if (low < lowest) {
                            lowest = low;
                        }

                        if (high > highest) {
                            highest = high;
                            highestIsIntermediate = j == 0;
                        }

                        if (low == 0 && high == 1) {
                            break outer;
                        }
                    }
                }
            }

            if (Float.isFinite(highest)) {
                double ceiling = adjustedY + lowest;

                if (ceiling - lastTargetY > adjustedHeight) {
                    newY = lastTargetY;
                    break;
                }

                lastTargetY = adjustedY + highest;

                if (lastTargetY - adjustedY > jumpHeight) {
                    return Float.NaN;
                }
            } else if ((adjustedY + 1) - lastTargetY > adjustedHeight) {
                newY = lastTargetY;
                break;
            }
        }

        if (newY > adjustedY) {
            if (checkJump(obx, mbx, obz, mbz, aox, adjustedY, aoz, newY)) {
                return Float.NaN;
            }

            if (!highestIsIntermediate) {
                return (float) (newY - Math.floor(newY));
            }
        }

        double finalY = checkFall(obx, mbx, obz, mbz, oby, aox, adjustedY, aoz);
        if (Double.isNaN(finalY)) {
            return Float.NaN;
        }

        return (float) (finalY - Math.floor(finalY));
    }

    @Override
    public boolean checkDiagonal(int x, int y, int z, int tx, int tz, float nodeOffset) {
        int dx = tx - x;
        int dz = tz - z;

        double aox = x + 0.5 - halfWidth;
        double aoz = z + 0.5 - halfWidth;

        double amx = aox + adjustedWidth;
        double amz = aoz + adjustedWidth;

        int sx = (int) Math.floor(aox + Math.min(0, dx));
        int ex = (int) Math.floor(amx + Math.abs(dx));

        int sz = (int) Math.floor(aoz + Math.min(0, dz));
        int ez = (int) Math.floor(amz + Math.abs(dz));

        boolean xf = isFull(dx, x, amx);
        int xo = computeOffset(dx, x, amx);
        int sdx = (int)Math.signum(dx);

        boolean zf = isFull(dz, z, amz);
        int zo = computeOffset(dz, z, amz);
        int sdz = (int)Math.signum(dz);

        double adjustedY = y + nodeOffset;
        int requiredHeight = ((int)Math.floor(adjustedY + height)) - y + 1;

        boolean limitMinX = dx < 0;
        boolean limitMaxX = dx > 0;

        for (int i = 0; i < requiredHeight; i++) {
            int by = y + i;

            for (int j = xf ? 1 : 0; j < (sx == ex ? 1 : 2); j++) {
                int bx = xo + j * sdx;

                for (int bz = sz; bz <= ez; bz++) {
                    if (hasDiagonal(bx, by, bz, j, aox, adjustedY, aoz, adjustedWidth, adjustedHeight,
                            adjustedWidth, dx, dz)) {
                        return false;
                    }
                }
            }

            for (int j = zf ? 1 : 0; j < (sz == ez ? 1 : 2); j++) {
                int bz = zo + j * sdz;

                for (int bx = limitMinX ? sx + 1 : sx; bx <= (limitMaxX ? ex - 1 : ex); bx++) {
                    if (hasDiagonal(bx, by, bz, j, aox, adjustedY, aoz, adjustedWidth, adjustedHeight,
                            adjustedWidth, dx, dz)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean checkJump(int obx, int mbx, int obz, int mbz, double aox, double aoy, double aoz,
            double targetHeight) {
        double exactHeight = aoy + height;
        int ceilHeight = (int)Math.ceil(exactHeight);
        int jumpSearch = (int)Math.ceil(targetHeight - aoy);

        for (int i = exactHeight == ceilHeight ? 0 : -1; i < jumpSearch; i++) {
            int by = ceilHeight + i;

            for (int bx = obx; bx <= mbx; bx++) {
                for (int bz = obz; bz <= mbz; bz++) {
                    Solid solid = space.solidAt(bx, by, bz);
                    if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                        continue;
                    }

                    if (solid.isFull()) {
                        return true;
                    }

                    Bounds3D closest = solid.closestCollision(bx, by, bz, aox, aoy, aoz, adjustedWidth,
                            adjustedHeight, adjustedWidth, Direction.UP, jumpHeight);
                    if (closest != null && by + closest.originY() - targetHeight < height) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private double checkFall(int obx, int mbx, int obz, int mbz, int oby, double aox, double aoy, double aoz) {
        boolean full = oby == aoy;
        if (full && !walk) {
            return aoy;
        }

        for (int i = full ? 0 : -1; i < fallSearchHeight; i++) {
            int by = oby - (i + 1);

            double highestY = checkDownwardLayer(obx, mbx, obz, mbz, by, i, aox, aoy, aoz);
            if (!walk) {
                return Double.isFinite(highestY) ? by + highestY : by;
            }

            if (!Double.isFinite(highestY)) {
                continue;
            }

            double target = by + highestY;
            double fall = aoy - target;
            if (fall > fallTolerance) {
                return Double.NaN;
            }

            return target;
        }

        return Double.NaN;
    }

    private double checkDownwardLayer(int startX, int endX, int startZ, int endZ, int by, int i,
            double ox, double oy, double oz) {
        double highestY = Double.NEGATIVE_INFINITY;

        for (int bx = startX; bx <= endX; bx++) {
            for (int bz = startZ; bz <= endZ; bz++) {
                Solid solid = space.solidAt(bx, by, bz);
                if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                    continue;
                }

                if (solid.isFull()) {
                    return 1;
                }

                Bounds3D bounds = solid.closestCollision(bx, by, bz, ox, oy, oz, adjustedWidth, adjustedHeight,
                        adjustedWidth, Direction.DOWN, fallSearchHeight);
                if (bounds != null) {
                    double height = bounds.maxY();
                    if (height == 1) {
                        return 1;
                    }

                    if (height > highestY) {
                        highestY = height;
                    }
                }
            }
        }

        return highestY;
    }

    private long diagonalMinMax(int bx, int by, int bz, int i, double aox, double aoy, double aoz, double alx,
            double aly, double alz, double dx, double dz) {
        Solid solid = space.solidAt(bx, by, bz);

        if (solid.isEmpty() || (i == 0 && solid.isFull())) {
            return Solid.NO_COLLISION;
        }

        if (solid.isFull()) {
            return Solid.result(0, 1);
        }

        return solid.minMaxCollision(bx, by, bz, aox, aoy, aoz, alx, aly, alz, dx, 0, dz);
    }

    private boolean hasDiagonal(int bx, int by, int bz, int i, double aox, double aoy, double aoz, double alx,
            double aly, double alz, double dx, double dz) {
        Solid solid = space.solidAt(bx, by, bz);

        if (solid.isEmpty() || (i == 0 && solid.isFull())) {
            return false;
        }

        if (solid.isFull()) {
            return true;
        }

        return solid.hasCollision(bx, by, bz, aox, aoy, aoz, alx, aly, alz, dx, 0, dz);
    }

    private int computeJumpSearchHeight(int blockY, double actualY) {
        return blockY == actualY ? searchHeight :
                ((int)Math.ceil(actualY + height + jumpHeight) - blockY) + 1;
    }

    private static int computeOffset(double d, double oc, double mc) {
        return d < 0 ? (int)Math.floor(oc) : (int)Math.floor(mc);
    }

    private static boolean isFull(double d, double oc, double mc) {
        return d < 0 ? oc == Math.rint(oc) : mc == Math.rint(mc);
    }
}