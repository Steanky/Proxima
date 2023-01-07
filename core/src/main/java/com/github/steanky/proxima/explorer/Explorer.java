package com.github.steanky.proxima.explorer;

import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Explorer {
    void exploreEach(@NotNull Node currentNode, @NotNull NodeHandler handler, @NotNull Vec3I2ObjectMap<Node> graph);
}