package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
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

        if(bounds.length == 1) {
            return new Solid12(bounds[0]);
        }

        if (bounds.length == 2) {
            return new Solid12(bounds[0], bounds[1]);
        }

        return new SolidN(bounds);
    }
}
