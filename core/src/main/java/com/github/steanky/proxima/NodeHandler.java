package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NodeHandler {
    void handle(@NotNull Node node, int x, int y, int z);
}
