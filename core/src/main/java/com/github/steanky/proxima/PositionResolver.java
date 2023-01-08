package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PositionResolver {
    @NotNull Vec3I resolve(double x, double y, double z);

    default @NotNull Vec3I resolve(@NotNull Vec3D vec) {
        return resolve(vec.x(), vec.y(), vec.z());
    }

    PositionResolver FLOOR = Vec3I::immutableFloored;
}
