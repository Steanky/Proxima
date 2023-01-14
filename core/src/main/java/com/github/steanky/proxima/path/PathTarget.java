package com.github.steanky.proxima.path;

import com.github.steanky.proxima.resolver.PositionResolver;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public interface PathTarget {
    static @NotNull PathTarget coordinate(int x, int y, int z) {
        Vec3I vector = Vec3I.immutable(x, y, z);
        return () -> vector;
    }

    static @NotNull PathTarget coordinate(@NotNull Vec3I vector) {
        Objects.requireNonNull(vector);
        return () -> vector;
    }

    static @NotNull PathTarget resolving(@NotNull Supplier<Vec3D> vectorSupplier, @NotNull PositionResolver resolver) {
        Objects.requireNonNull(vectorSupplier);
        Objects.requireNonNull(resolver);

        return () -> {
            Vec3D position = vectorSupplier.get();
            if (position != null) {
                return resolver.resolve(position);
            }

            return null;
        };
    }

    @Nullable Vec3I resolve();
}