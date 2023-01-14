package com.github.steanky.proxima.resolver;

import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PositionResolver {
    @NotNull Vec3I resolve(double x, double y, double z);

    default @NotNull Vec3I resolve(@NotNull Vec3D vec) {
        return resolve(vec.x(), vec.y(), vec.z());
    }

    PositionResolver FLOORED = Vec3I::immutableFloored;

    static @NotNull PositionResolver seekBelow(@NotNull Space space, int searchHeight, double width, double epsilon) {
        return new ClosestBelow(space, searchHeight, width, epsilon);
    }
}
