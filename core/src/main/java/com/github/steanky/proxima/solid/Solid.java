package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Represents an object in 3D voxel space. Conceptually, solids consist of zero or more three-dimensional bounding
 * boxes. If a solid has no bounds, it will not be collided with (see {@link Solid#EMPTY}).
 * <p>
 * Solid instances can be created using {@link Solid#of(Bounds3D...)} or any associated overloads. In all cases, none of
 * the bounds of the solid may possess an origin vector with a component less than 0, <i>or</i> a bound in which the
 * sum of the origin and lengths vector exceeds 1. Attempting to create such solids will throw a
 * {@link IllegalArgumentException}.
 */
public sealed interface Solid permits SolidImpl {
    /**
     * The shared, empty Solid. It is encouraged to call {@link Solid#isEmpty()}, rather than doing a reference
     * comparison with this field, as it is not guaranteed to be singleton.
     */
    Solid EMPTY = new SolidImpl();

    /**
     * The shared, full Solid. It is encouraged to call {@link Solid#isFull()}, rather than doing a reference comparison
     * with this field, as it is not guaranteed to be singleton.
     */
    Solid FULL = new SolidImpl(Bounds3D.immutable(0, 0, 0, 1, 1, 1));

    /**
     * Returned by {@link Solid#minMaxCollision(int, int, int, double, double, double, double, double, double,
     * Direction, double, double)} and {@link Solid#minMaxCollision(int, int, int, double, double, double, double,
     * double, double, double, double, double, double)} to indicate no collision was found. The higher 32 bits represent
     * single-precision positive infinity, whereas the lower 32 bits represent single-precision negative infinity.
     */
    long NO_COLLISION = 0x7F80_0000_FF80_0000L;

    /**
     * Packs two floats into a single long. This is used by
     * {@link Solid#minMaxCollision(int, int, int, double, double, double, double, double, double, Direction, double, double)}
     * and
     * {@link Solid#minMaxCollision(int, int, int, double, double, double, double, double, double, double, double, double, double)}
     * to return the highest and lowest colliding points of a solid.
     * @param lowest the lowest float
     * @param highest the highest float
     * @return a long containing the two values
     */
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
        if (bounds.originX() == 0 && bounds.originY() == 0 && bounds.originZ() == 0
                && bounds.lengthX() == 1 && bounds.lengthY() == 1 && bounds.lengthZ() == 1) {
            return FULL;
        }

        return new SolidImpl(bounds);
    }

    static @NotNull Solid of(@NotNull Bounds3D first, @NotNull Bounds3D second) {
        return new SolidImpl(first, second);
    }

    static @NotNull Solid of(@NotNull Bounds3D @NotNull ... bounds) {
        if (bounds.length == 0) {
            return of();
        }

        if (bounds.length == 1) {
            return of(bounds[0]);
        }

        if (bounds.length == 2) {
            return of(bounds[0], bounds[1]);
        }

        return new SolidImpl(bounds);
    }

    Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    @NotNull @Unmodifiable List<Bounds3D> children();

    long minMaxCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, @NotNull Direction d, double l, double e);

    @Nullable Bounds3D closestCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, @NotNull Direction d, double l, double e);

    boolean hasCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, @NotNull Direction d, double l, double e);

    boolean hasCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, double dx, double dy, double dz, double e);

    long minMaxCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, double dx, double dy, double dz, double e);
}