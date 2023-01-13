package com.github.steanky.proxima.node;

import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NodeProcessor {
    void processPath(@NotNull Node head, @NotNull Vec3I2ObjectMap<Node> graph);

    NodeProcessor NO_CHANGE = (head, graph) -> {};

    static @NotNull NodeProcessor createDiagonals(@NotNull NodeSnapper nodeSnapper) {
        return new DiagonalProcessor(nodeSnapper);
    }
}
