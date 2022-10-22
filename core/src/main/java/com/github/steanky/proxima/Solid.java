package com.github.steanky.proxima;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;

public interface Solid {
    @NotNull Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    int x();

    int y();

    int z();
}
