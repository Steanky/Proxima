package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IPredicate;
import org.jetbrains.annotations.NotNull;

public interface PathSettings {
    @NotNull Vec3IPredicate successPredicate();

    @NotNull Explorer explorer();

    @NotNull Heuristic heuristic();

    @NotNull Vec3I2ObjectMap<Node> graph();

    @NotNull Object key();
}
