package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Explorer {
    void exploreEach(@NotNull Node node, @NotNull NodeHandler handler);
}
