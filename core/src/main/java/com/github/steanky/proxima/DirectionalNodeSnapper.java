package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import org.jetbrains.annotations.NotNull;

public interface DirectionalNodeSnapper {
    void snap(@NotNull Direction direction, @NotNull Node currentNode, @NotNull NodeHandler handler);

    boolean canSkip(@NotNull Node currentNode, @NotNull Node parent, @NotNull Direction direction);
}
