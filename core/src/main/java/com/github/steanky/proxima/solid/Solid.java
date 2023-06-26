package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Represents an object in 3D voxel space. Conceptually, solids consist of zero or more three-dimensional bounding
 * boxes. If a solid has no bounds, it will not be collided with (see {@link Solid#EMPTY}).
 * <p>
 * None of the bounds of a solid may exceed a 1x1x1 cube. Using implementations that allow this may result in undefined
 * pathfinding behavior.
 * <p>
 * This class provides several default methods for performing various kinds of collision checking. These are, among
 * other things, used during pathfinding to determine navigability of candidate nodes. By default, these methods
 * delegate to those in {@link Util}, which relies on the {@link Solid#children()} method to iterate the individual
 * bounding boxes. Implementations may override these in order to use platform-native collision checking functionality,
 * if appropriate.
 */
public interface Solid {
    /**
     * The shared, empty Solid. It is encouraged to call {@link Solid#isEmpty()}, rather than doing an equality
     * comparison with this field, as it is not guaranteed to be singleton.
     */
    Solid EMPTY = new Solid() {
        @Override
        public @NotNull Bounds3D bounds() {
            throw new IllegalStateException("No bounds for empty solids");
        }

        @Override
        public boolean isFull() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public @NotNull @Unmodifiable List<Bounds3D> children() {
            return List.of();
        }
    };

    /**
     * The shared, full Solid. It is encouraged to call {@link Solid#isFull()}, rather than doing an equality comparison
     * with this field, as it is not guaranteed to be singleton.
     */
    Solid FULL = new Solid12(Bounds3D.immutable(0, 0, 0, 1, 1, 1));

    /**
     * Returned by {@link Solid#minMaxCollision(int, int, int, double, double, double, double, double, double,
     * Direction, double, double)} and {@link Solid#minMaxCollision(int, int, int, double, double, double, double,
     * double, double, double, double, double, double)} to indicate no collision was found. The higher 32 bits represent
     * single-precision positive infinity, whereas the lower 32 bits represent single-precision negative infinity.
     */
    long NO_COLLISION = 0x7F80_0000_FF80_0000L;

    /**
     * Returned by {@link Solid#minMaxCollision(int, int, int, double, double, double, double, double, double,
     * Direction, double, double)} and {@link Solid#minMaxCollision(int, int, int, double, double, double, double,
     * double, double, double, double, double, double)} to indicate that collision checking failed; passability in this
     * direction should not be possible, and {@link NodeSnapper} implementations should quickly exit instead of checking
     * additional blocks. The default collision implementations in {@link Util} never return this value.
     */
    long FAIL = 0xFFC0_0001_FFC0_0001L;

    static long result(float lowest, float highest) {
        int hi = Float.floatToRawIntBits(lowest);
        int lo = Float.floatToRawIntBits(highest);
        return (((long) hi) << 32) | (lo & 0xFFFF_FFFFL);
    }

    static float lowest(long collisionResult) {
        return Float.intBitsToFloat((int) (collisionResult >>> 32));
    }

    static float highest(long collisionResult) {
        return Float.intBitsToFloat((int) collisionResult);
    }

    static @NotNull Solid of() {
        return EMPTY;
    }

    static @NotNull Solid of(@NotNull Bounds3D bounds) {
        return new Solid12(bounds);
    }

    static @NotNull Solid of(@NotNull Bounds3D first, @NotNull Bounds3D second) {
        return new Solid12(first, second);
    }

    static @NotNull Solid of(@NotNull Bounds3D @NotNull ... bounds) {
        if (bounds.length == 0) {
            return EMPTY;
        }

        if (bounds.length == 1) {
            return new Solid12(bounds[0]);
        }

        if (bounds.length == 2) {
            return new Solid12(bounds[0], bounds[1]);
        }

        return new SolidN(bounds);
    }

    @NotNull Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    @NotNull @Unmodifiable List<Bounds3D> children();

    default long minMaxCollision(int x, int y, int z, double ox, double oy, double oz, double lx, double ly, double lz, @NotNull Direction d, double l, double e) {
        return Util.minMaxCollision(this, x, y, z, ox, oy, oz, lx, ly, lz, d, l, e);
    }

    default @Nullable Bounds3D closestCollision(int x, int y, int z, double ox, double oy, double oz, double lx, double ly, double lz, @NotNull Direction d, double l, double e) {
        return Util.closestCollision(this, x, y, z, ox, oy, oz, lx, ly, lz, d, l, e);
    }

    default boolean hasCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, @NotNull Direction d, double l, double e) {
        return Util.hasCollision(this, x, y, z, cx, cy, cz, lx, ly, lz, d, l, e);
    }

    default boolean hasCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, double dx, double dy, double dz, double e) {
        return Util.hasCollision(this, x, y, z, cx, cy, cz, lx, ly, lz, dx, dy, dz, e);
    }

    default long minMaxCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, double dx, double dy, double dz, double e) {
        return Util.minMaxCollision(this, x, y, z, cx, cy, cz, lx, ly, lz, dx, dy, dz, e);
    }
}