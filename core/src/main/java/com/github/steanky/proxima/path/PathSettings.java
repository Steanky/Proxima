package com.github.steanky.proxima.path;

import com.github.steanky.proxima.Explorer;
import com.github.steanky.proxima.Heuristic;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IBiPredicate;
import org.jetbrains.annotations.NotNull;

public interface PathSettings {
    @NotNull Vec3IBiPredicate successPredicate();

    @NotNull Explorer explorer();

    @NotNull Heuristic heuristic();

    @NotNull Vec3I2ObjectMap<Node> graph();
}
