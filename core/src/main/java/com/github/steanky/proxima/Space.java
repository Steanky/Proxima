package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Space {
    @NotNull Solid solidAt(int x, int y, int z);

    default @NotNull Solid solidAt(@NotNull Vec3I vec) {
        return solidAt(vec.x(), vec.y(), vec.z());
    }
}
