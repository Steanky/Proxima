package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Explorer {
    void exploreEach(@NotNull Node currentNode, int goalX, int goalY, int goalZ, @NotNull NodeHandler handler,
            @NotNull Vec3I2ObjectMap<Node> graph);
}
