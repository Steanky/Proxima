package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface Agent {
    @NotNull @Unmodifiable List<Vec3I> explorationFront(@NotNull Direction direction);
}
