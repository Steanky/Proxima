package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

public interface DirectionalNodeSnapper {
    void snap(@NotNull Direction direction, @NotNull Node node, @NotNull NodeHandler handler);
}
