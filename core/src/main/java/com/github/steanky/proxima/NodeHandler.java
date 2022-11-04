package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NodeHandler {
    void handle(@NotNull Node node, @NotNull Movement movement, int x, int y, int z);
}
