package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;

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
        public boolean overlaps(double ox, double oy, double oz, double lx, double ly, double lz) {
            return false;
        }

        @Override
        public boolean expandOverlaps(double ox, double oy, double oz, double lx, double ly, double lz, double ex,
                double ey, double ez) {
            return false;
        }
    };

    Solid FULL = new SingletonSolid(Bounds3D.immutable(0, 0, 0, 1, 1, 1));

    @NotNull Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    boolean overlaps(double ox, double oy, double oz, double lx, double ly, double lz);

    boolean expandOverlaps(double ox, double oy, double oz, double lx, double ly, double lz, double ex,
            double ey, double ez);
}
