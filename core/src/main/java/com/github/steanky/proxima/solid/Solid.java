package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Solid {
    enum Order {
        HIGHEST,
        LOWEST,
        NONE
    }

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
        public @Nullable Bounds3D overlaps(double ox, double oy, double oz, double lx, double ly, double lz,
                @NotNull Solid.Order order) {
            return null;
        }

        @Override
        public @Nullable Bounds3D expandOverlaps(double ox, double oy, double oz, double lx, double ly, double lz,
                double ex, double ey, double ez, @NotNull Solid.Order order) {
            return null;
        }
    };

    Solid FULL = new SingletonSolid(Bounds3D.immutable(0, 0, 0, 1, 1, 1));

    @NotNull Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    @Nullable Bounds3D overlaps(double ox, double oy, double oz, double lx, double ly, double lz,
            @NotNull Solid.Order order);

    @Nullable Bounds3D expandOverlaps(double ox, double oy, double oz, double lx, double ly, double lz, double ex,
            double ey, double ez, @NotNull Solid.Order order);

    static @NotNull Solid of(Bounds3D bounds) {
        return new SingletonSolid(bounds);
    }

    static @NotNull Solid of(Bounds3D... bounds) {
        if (bounds.length == 0) {
            return EMPTY;
        }

        if(bounds.length == 1) {
            return new SingletonSolid(bounds[0]);
        }

        return new CompositeSolid(bounds);
    }
}
