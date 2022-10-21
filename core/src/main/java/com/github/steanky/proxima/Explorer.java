package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

public interface Explorer {
    void exploreEach(@NotNull Node node, @NotNull NodeHandler handler);
}
