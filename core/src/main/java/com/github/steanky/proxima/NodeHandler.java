package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface NodeHandler {
    void handle(@NotNull Node node, @Nullable Node targetNode, int x, int y, int z, float yOffset, boolean bidirectional);
}
