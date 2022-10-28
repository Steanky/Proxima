package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

public interface PathPostProcessor {
    @NotNull NavigationResult process(@NotNull PathResult pathResult);
}
