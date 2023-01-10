package com.github.steanky.proxima.resolver;

import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface PositionResolver {
    record OffsetPosition(@NotNull Vec3I vector, float offset) {
        public OffsetPosition {
            Objects.requireNonNull(vector);
            if (offset < 0 || offset > 1) {
                throw new IllegalArgumentException("Invalid offset " + offset + ", must be in range [0, 1]");
            }
        }
    }

    @NotNull OffsetPosition resolve(double x, double y, double z);

    default @NotNull OffsetPosition resolve(@NotNull Vec3D vec) {
        return resolve(vec.x(), vec.y(), vec.z());
    }

    PositionResolver FLOORED =
            (x, y, z) -> new OffsetPosition(Vec3I.immutableFloored(x, y, z), (float) (y - Math.floor(y)));
}
