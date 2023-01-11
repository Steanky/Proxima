package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface Solid {
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

    Solid FULL = new Solid12(Bounds3D.immutable(0, 0, 0, 1, 1, 1));

    @NotNull Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    @NotNull @Unmodifiable List<Bounds3D> children();

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

    /**
     * Returned by {@link Solid#minMaxCollision(int, int, int, double, double, double, double, double, double,
     * Direction, double)} to indicate no collision was found. The higher 32 bits represent single-precision positive
     * infinity, whereas the lower 32 bits represent single-precision negative infinity.
     */
    long NO_COLLISION = 0x7F80_0000_FF80_0000L;

    default long minMaxCollision(int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, @NotNull Direction d, double l) {
        return Util.minMaxCollision(this, x, y, z, ox, oy, oz, lx, ly, lz, d, l);
    }

    default @Nullable Bounds3D closestCollision(int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, @NotNull Direction d, double l) {
        return Util.closestCollision(this, x, y, z, ox, oy, oz, lx, ly, lz, d, l);
    }

    default boolean hasCollision(int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, @NotNull Direction d, double l) {
        return Util.hasCollision(this, x, y, z, ox, oy, oz, lx, ly, lz, d, l);
    }

    default boolean hasCollision(int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, double dx, double dy, double dz) {
        return false;
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
}