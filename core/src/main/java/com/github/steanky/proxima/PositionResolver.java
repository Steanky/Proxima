package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

public interface PositionResolver {
    @NotNull Vec3I resolve(double x, double y, double z);
}
