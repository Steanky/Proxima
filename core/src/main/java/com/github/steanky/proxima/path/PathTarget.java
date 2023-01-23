package com.github.steanky.proxima.path;

import com.github.steanky.proxima.resolver.PositionResolver;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public interface PathTarget {
    static @NotNull PathTarget coordinate(int x, int y, int z) {
        Vec3I vector = Vec3I.immutable(x, y, z);
        return new PathTarget() {
            @Override
            public @NotNull Vec3I resolve() {
                return vector;
            }

            @Override
            public boolean hasChanged() {
                return false;
            }
        };
    }

    static @NotNull PathTarget coordinate(@NotNull Vec3I vector) {
        Objects.requireNonNull(vector);
        return new PathTarget() {
            private Vec3I previous = vector.immutable();

            @Override
            public @NotNull Vec3I resolve() {
                return vector;
            }

            @Override
            public boolean hasChanged() {
                Vec3I previous = this.previous;
                this.previous = vector.immutable();

                return !previous.equals(vector);
            }
        };
    }

    static @NotNull PathTarget resolving(@NotNull Supplier<Vec3D> vectorSupplier, @NotNull PositionResolver resolver,
            @NotNull BiPredicate<Vec3D, Vec3D> changeDetector) {
        Objects.requireNonNull(vectorSupplier);
        Objects.requireNonNull(resolver);
        Objects.requireNonNull(changeDetector);

        return new PathTarget() {
            private Vec3D lastPosition;

            @Override
            public @Nullable Vec3I resolve() {
                Vec3D newPosition = vectorSupplier.get();
                if (newPosition != null) {
                    return resolver.resolve(newPosition);
                }

                return null;
            }

            @Override
            public boolean hasChanged() {
                Vec3D lastPosition = this.lastPosition;
                Vec3D newPosition = vectorSupplier.get();

                if (lastPosition == null) {
                    boolean changed = newPosition != null;
                    if (changed) {
                        this.lastPosition = newPosition;
                    }

                    return changed;
                }

                if (newPosition == null) {
                    this.lastPosition = null;
                    return true;
                }

                if (changeDetector.test(lastPosition, newPosition)) {
                    this.lastPosition = newPosition;
                    return true;
                }

                return false;
            }
        };
    }

    @Nullable Vec3I resolve();

    boolean hasChanged();
}