package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface Solid {
    Bounds3D[] EMPTY_BOUNDS_ARRAY = new Bounds3D[0];

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
        public @NotNull @Unmodifiable List<Bounds3D> children() {
            return List.of();
        }
    };

    Solid FULL = new SingletonSolid(Bounds3D.immutable(0, 0, 0, 1, 1, 1));

    @NotNull Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    @NotNull @Unmodifiable List<Bounds3D> children();

    static @NotNull Solid of(@NotNull Bounds3D bounds) {
        return new SingletonSolid(bounds);
    }

    static @NotNull Solid of(Bounds3D @NotNull ... bounds) {
        if (bounds.length == 0) {
            return EMPTY;
        }

        if(bounds.length == 1) {
            return new SingletonSolid(bounds[0]);
        }

        return new CompositeSolid(bounds);
    }
}
