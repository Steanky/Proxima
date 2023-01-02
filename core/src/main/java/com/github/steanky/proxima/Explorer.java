package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Explorer {
    void exploreEach(@NotNull Node currentNode, int goalX, int goalY, int goalZ, @NotNull NodeHandler handler);
}
