package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Vec3D;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BasicNodeSnapper implements NodeSnapper {
    private static final double INITIAL_SEARCH_LIMIT = 2;

    private final double fallTolerance;

    private final int searchHeight;
    private final int fallSearchHeight;

    private final boolean fullWidth;

    private final int halfBlockWidth;

    private final double jumpHeight;
    private final Space space;
    private final boolean walk;

    private final double width;
    private final double halfWidth;
    private final double height;

    private final double epsilon;

    private BasicNodeSnapper(@NotNull Space space, double width, double height, double fallTolerance, double jumpHeight,
            boolean walk, double epsilon) {
        validate(width, height, fallTolerance, jumpHeight, epsilon);

        int rWidth = (int) Math.rint(width);

        this.fullWidth = width == rWidth && (rWidth & 1) != 0;
        this.fallTolerance = fallTolerance;

        //silly, totally unnecessary way to add 1 to a number only if it is even
        int blockWidth = ((int) Math.ceil(width)) | 1;

        this.searchHeight = (int) Math.ceil(height + jumpHeight);
        this.fallSearchHeight = (int) Math.ceil(fallTolerance) + 1;

        this.halfBlockWidth = blockWidth / 2;
        this.jumpHeight = jumpHeight;
        this.space = Objects.requireNonNull(space);

        this.width = width;
        this.halfWidth = width / 2;
        this.height = height;

        this.walk = walk;

        this.epsilon = epsilon;
    }

    public BasicNodeSnapper(@NotNull Space space, double width, double height, double epsilon) {
        this(space, width, height, 0, 0, false, epsilon);
    }

    public BasicNodeSnapper(@NotNull Space space, double width, double height, double fallTolerance, double jumpHeight, double epsilon) {
        this(space, width, height, fallTolerance, jumpHeight, true, epsilon);
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

    private static int computeOffset(double d, double originCoordinate, double maxCoordinate, double epsilon) {
        return d < 0 ? (int) Math.floor(originCoordinate) : (int) Math.floor(maxCoordinate - epsilon);
    }

    private long snapVertical(Direction direction, int nodeX, int nodeY, int nodeZ, float nodeOffset) {
        double exactY = nodeY + nodeOffset;

        double ax = nodeX + 0.5 - halfWidth;
        double az = nodeZ + 0.5 - halfWidth;

        if (direction == Direction.UP) {
            double exactHeight = height + nodeOffset;
            int ceilHeight = (int) Math.ceil(exactHeight);
            int offset = ceilHeight + nodeY;
            boolean fullHeight = exactHeight == ceilHeight;

            for (int i = fullHeight ? 0 : -1; i < 1; i++) {
                int y = i + offset;

                for (int dex = -halfBlockWidth; dex <= halfBlockWidth; dex++) {
                    for (int dez = -halfBlockWidth; dez <= halfBlockWidth; dez++) {
                        int x = nodeX + dex;
                        int z = nodeZ + dez;

                        Solid solid = space.solidAt(x, y, z);
                        if (solid == null) {
                            return FAIL;
                        }

                        if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                            continue;
                        }

                        if (solid.hasCollision(x, y, z, ax, exactY, az, width, height, width, Direction.UP, 1, epsilon)) {
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
                    if (solid == null) {
                        return FAIL;
                    }

                    if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                        continue;
                    }

                    if (solid.hasCollision(x, y, z, ax, exactY, az, width, height, width, Direction.DOWN, 1, epsilon)) {
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
                    if (solid == null) {
                        return FAIL;
                    }

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

                    long res =
                            solid.minMaxCollision(x, y, z, ax, exactY, az, width, height + jumpHeight, width,
                                    direction, 1, epsilon);
                    if (res == Solid.FAIL) {
                        return FAIL;
                    }

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

                if (ceiling - lastTargetY + epsilon > height) {
                    newY = lastTargetY;
                    break;
                }

                lastTargetY = y + highest;

                //too high to make this jump
                if (lastTargetY - exactY > jumpHeight + epsilon) {
                    return FAIL;
                }
            } else if ((y + 1) - lastTargetY + epsilon >= height) {
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

        int oby = (int) Math.floor(newY);
        boolean full = newY == oby;

        if (full && !walk) {
            //full, non-flying entities won't fall at all, and we don't need to compute their offset since it's known
            return NodeSnapper.encode(newY, false, 0);
        }

        int obx = nx - halfBlockWidth;
        int mbx = nx + halfBlockWidth;

        int obz = nz - halfBlockWidth;
        int mbz = nz + halfBlockWidth;

        double nax = ax + dx;
        double naz = az + dz;

        //for walking entities, check blocks below the target
        //for flying entities, check the current block (which is non-full) and use its offset
        double finalY = checkFall(obx, mbx, obz, mbz, oby, nax, newY, naz);
        if (Double.isNaN(finalY)) {
            return FAIL;
        }

        boolean intermediate = finalY != newY && highestIsIntermediate;
        return NodeSnapper.encode(finalY, intermediate, intermediate ? (float) (newY - finalY) : 0F);
    }

    @Override
    public long checkInitial(double x, double y, double z, int tx, int ty, int tz) {
        double cbx = tx + 0.5;
        double cbz = tz + 0.5;

        if (Vec3D.distanceSquared(x, y, z, cbx, y, cbz) > INITIAL_SEARCH_LIMIT) {
            return FAIL;
        }

        double aox = x - halfWidth;
        double aoz = z - halfWidth;

        double amx = x + halfWidth;
        double amz = z + halfWidth;

        int obx = (int) Math.floor(aox);
        int oby = (int) Math.floor(y);
        int obz = (int) Math.floor(aoz);

        int mbx = (int) Math.floor(amx);
        int mbz = (int) Math.floor(amz);

        double exactY = checkFall(obx, mbx, obz, mbz, oby, aox, y, aoz);
        if (Double.isNaN(exactY)) {
            //invalid start location
            return FAIL;
        }

        double dx = cbx - x;
        double dz = cbz - z;

        boolean cx = dx != 0;
        boolean cz = dz != 0;

        int firstX = (int) Math.floor(x + halfWidth * Math.signum(dx));
        int firstZ = (int) Math.floor(z + halfWidth * Math.signum(dz));

        int sx = Math.min(firstX, tx);
        int ex = Math.max(firstX, tx);

        int sz = Math.min(firstZ, tz);
        int ez = Math.max(firstZ, tz);

        int adjustedBlockY = (int) Math.floor(exactY);

        int actualSearchHeight = computeJumpSearchHeight(adjustedBlockY, exactY);

        boolean limitMinX = cx && dx < 0;
        boolean limitMaxX = cx && dx > 0;

        int xo = computeOffset(dx, aox, amx, epsilon);
        int sdx = (int) Math.signum(dx);

        int zo = computeOffset(dz, aoz, amz, epsilon);
        int sdz = (int) Math.signum(dz);

        double newY = Double.NaN;
        double lastTargetY = exactY;

        boolean highestIsIntermediate = false;

        for (int i = 0; i < actualSearchHeight; i++) {
            int by = adjustedBlockY + i;

            float lowest = Float.POSITIVE_INFINITY;
            float highest = Float.NEGATIVE_INFINITY;

            if (cx) {
                outer:
                for (int j = 0; j < (sx == ex || (dx > 0 ? mbx : obx) == ex ? 1 : 2); j++) {
                    int bx = xo + j * sdx;
                    boolean xs = bx == (dx < 0 ? obx : mbx);

                    for (int bz = sz; bz <= ez; bz++) {
                        long res = diagonalMinMax(bx, by, bz, xs && bz == (dz < 0 ? obz : mbz), aox, exactY, aoz, width, height + jumpHeight,
                                width, dx, dz);
                        if (res == Solid.FAIL) {
                            return FAIL;
                        }

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
                for (int j = 0; j < (sz == ez || (dz > 0 ? mbz : obz) == ez ? 1 : 2); j++) {
                    int bz = zo + j * sdz;
                    boolean zs = bz == (dz < 0 ? obz : mbz);

                    for (int bx = limitMinX ? sx + 1 : sx; bx <= (limitMaxX ? ex - 1 : ex); bx++) {
                        long res = diagonalMinMax(bx, by, bz, zs && bx == (bx < 0 ? obx : mbx), aox, exactY, aoz, width, height + jumpHeight,
                                width, dx, dz);
                        if (res == Solid.FAIL) {
                            return FAIL;
                        }

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

            //if we found a solid this layer, check the gap below it
            if (Float.isFinite(highest)) {
                double ceiling = by + lowest;

                if (ceiling - lastTargetY + epsilon > height) {
                    newY = lastTargetY;
                    break;
                }

                lastTargetY = by + highest;

                //too high to make this jump
                if (lastTargetY - exactY > jumpHeight + epsilon) {
                    return FAIL;
                }
            } else if ((by + 1) - lastTargetY + epsilon >= height) {
                newY = lastTargetY;
                break;
            }
        }

        //newY was never assigned, so we can't move this direction
        if (Double.isNaN(newY)) {
            return FAIL;
        }

        double ax = x - halfWidth;
        double az = z - halfWidth;

        //jumping is necessary, so we need to check above us
        if (newY > exactY) {
            if (checkJump(obx, mbx, obz, mbz, ax, exactY, az, newY)) {
                return FAIL;
            }

            if (!highestIsIntermediate) {
                //if non-intermediate, we don't have to check below us to determine our actual height
                return NodeSnapper.encode(newY, false, 0);
            }
        }

        boolean full = newY == oby;
        if (full && !walk) {
            //full, non-flying entities won't fall at all, and we don't need to compute their offset since it's known
            return NodeSnapper.encode(newY, false, 0);
        }

        int nobx = tx - halfBlockWidth;
        int nmbx = tx + halfBlockWidth;

        int nobz = tz - halfBlockWidth;
        int nmbz = tz + halfBlockWidth;

        double nax = ax + dx;
        double naz = az + dz;

        //for walking entities, check blocks below the target
        //for flying entities, check the current block (which is non-full) and use its offset
        double finalY = checkFall(nobx, nmbx, nobz, nmbz, oby, nax, newY, naz);
        if (Double.isNaN(finalY)) {
            return FAIL;
        }

        boolean intermediate = finalY != newY && highestIsIntermediate;
        return NodeSnapper.encode(finalY, intermediate, intermediate ? (float) (newY - finalY) : 0F);
    }

    @Override
    public boolean checkDiagonal(int x, int y, int z, int tx, int tz, float nodeOffset) {
        int dx = tx - x;
        int dz = tz - z;

        double cx = x + 0.5;
        double cz = z + 0.5;

        double aox = cx - halfWidth;
        double aoz = cz - halfWidth;

        double amx = cx + halfWidth;
        double amz = cz + halfWidth;

        int obx = (int) Math.floor(aox);
        int obz = (int) Math.floor(aoz);

        int mbx = (int) Math.floor(amx);
        int mbz = (int) Math.floor(amz);

        int firstX = (int) Math.floor(cx + halfWidth * Math.signum(dx));
        int firstZ = (int) Math.floor(cz + halfWidth * Math.signum(dz));

        int sx = Math.min(firstX, tx);
        int ex = Math.max(firstX, tx);

        int sz = Math.min(firstZ, tz);
        int ez = Math.max(firstZ, tz);

        int xo = computeOffset(dx, x, amx, epsilon);
        int sdx = (int) Math.signum(dx);

        int zo = computeOffset(dz, z, amz, epsilon);
        int sdz = (int) Math.signum(dz);

        double adjustedY = y + nodeOffset;
        int requiredHeight = ((int) Math.floor(y + nodeOffset + height)) - y + 1;

        boolean limitMinX = dx < 0;
        boolean limitMaxX = dx > 0;

        for (int i = 0; i < requiredHeight; i++) {
            int by = y + i;

            if (dx != 0) {
                for (int j = 0; j < (sx == ex ? 1 : 2); j++) {
                    int bx = xo + j * sdx;
                    boolean xs = bx == (dx < 0 ? obx : mbx);

                    for (int bz = sz; bz <= ez; bz++) {
                        if (hasDiagonal(bx, by, bz, xs && bz == (dz < 0 ? obz : mbz), aox, adjustedY, aoz, width, height, width, dx, dz)) {
                            return false;
                        }
                    }
                }
            }

            if (dz != 0) {
                for (int j = 0; j < (sz == ez ? 1 : 2); j++) {
                    int bz = zo + j * sdz;
                    boolean zs = bz == (dz < 0 ? obz : mbz);

                    for (int bx = limitMinX ? sx + 2 : sx; bx <= (limitMaxX ? ex - 1 : ex); bx++) {
                        if (hasDiagonal(bx, by, bz, zs && bx == (bx < 0 ? obx : mbx), aox, adjustedY, aoz, width, height, width, dx, dz)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public @NotNull Space space() {
        return space;
    }

    private boolean checkJump(int obx, int mbx, int obz, int mbz, double aox, double aoy, double aoz, double targetHeight) {
        double exactHeight = aoy + height;
        int ceilHeight = (int) Math.ceil(exactHeight);
        int jumpSearch = (int) Math.ceil(targetHeight - aoy);

        for (int i = exactHeight == ceilHeight ? 0 : -1; i < jumpSearch; i++) {
            int by = ceilHeight + i;

            for (int bx = obx; bx <= mbx; bx++) {
                for (int bz = obz; bz <= mbz; bz++) {
                    Solid solid = space.solidAt(bx, by, bz);
                    if (solid == null) {
                        return true;
                    }

                    if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                        continue;
                    }

                    if (solid.isFull()) {
                        return true;
                    }

                    Bounds3D closest = solid.closestCollision(bx, by, bz, aox, aoy, aoz, width, height, width,
                            Direction.UP, jumpHeight, epsilon);
                    if (closest != null && by + closest.originY() - targetHeight < height - epsilon) {
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
                //only ever check a single layer if flying
                //if we find a block: use its offset
                //if we don't find a block: offset is 0
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

    private double checkDownwardLayer(int startX, int endX, int startZ, int endZ, int by, int i, double ox, double oy, double oz) {
        double highestY = Double.NEGATIVE_INFINITY;

        for (int bx = startX; bx <= endX; bx++) {
            for (int bz = startZ; bz <= endZ; bz++) {
                Solid solid = space.solidAt(bx, by, bz);
                if (solid == null) {
                    return Double.NEGATIVE_INFINITY;
                }

                if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                    continue;
                }

                if (solid.isFull()) {
                    return 1;
                }

                Bounds3D bounds =
                        solid.closestCollision(bx, by, bz, ox, oy, oz, width, height, width, Direction.DOWN,
                                fallSearchHeight, epsilon);
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

    private long diagonalMinMax(int bx, int by, int bz, boolean s, double aox, double aoy, double aoz, double alx, double aly, double alz, double dx, double dz) {
        Solid solid = space.solidAt(bx, by, bz);
        if (solid == null) {
            return Solid.FAIL;
        }

        if (solid.isEmpty() || (s && solid.isFull())) {
            return Solid.NO_COLLISION;
        }

        return solid.minMaxCollision(bx, by, bz, aox, aoy, aoz, alx, aly, alz, dx, 0, dz, epsilon);
    }

    private boolean hasDiagonal(int bx, int by, int bz, boolean s, double aox, double aoy, double aoz, double alx,
            double aly, double alz, double dx, double dz) {
        Solid solid = space.solidAt(bx, by, bz);
        if (solid == null) {
            return true;
        }

        if (solid.isEmpty() || (s && solid.isFull())) {
            return false;
        }

        return solid.hasCollision(bx, by, bz, aox, aoy, aoz, alx, aly, alz, dx, 0, dz, epsilon);
    }

    private int computeJumpSearchHeight(int blockY, double actualY) {
        return blockY == actualY ? searchHeight : ((int) Math.ceil(actualY + height + jumpHeight) - blockY) + 1;
    }
}