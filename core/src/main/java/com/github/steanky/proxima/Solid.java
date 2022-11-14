package com.github.steanky.proxima;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;

public interface Solid {
    Solid EMPTY = new Solid() {
        private static final Bounds3D EMPTY = Bounds3D.immutable(0, 0, 0, 0, 0, 0);

        @Override
        public @NotNull Bounds3D bounds() {
            return EMPTY;
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
    };

    Solid FULL = new Solid() {
        private static final Bounds3D FULL = Bounds3D.immutable(0, 0, 0, 1, 1, 1);

        @Override
        public @NotNull Bounds3D bounds() {
            return FULL;
        }

        @Override
        public boolean isFull() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean overlaps(double ox, double oy, double oz, double lx, double ly, double lz) {
            return false;
        }
    };

    @NotNull Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    boolean overlaps(double ox, double oy, double oz, double lx, double ly, double lz);
}
