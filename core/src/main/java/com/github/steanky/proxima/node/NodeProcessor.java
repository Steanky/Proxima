package com.github.steanky.proxima.node;

import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NodeProcessor {
    @NotNull Node processPath(@NotNull Node head, @NotNull Vec3I2ObjectMap<Node> graph);

    NodeProcessor NO_CHANGE = (head, graph) -> head;
}
