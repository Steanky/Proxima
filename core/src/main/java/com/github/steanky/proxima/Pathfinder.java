package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface Pathfinder {
    @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings, @NotNull Object regionKey);

    void registerRegion(@NotNull Object regionKey);

    void deregisterRegion(@NotNull Object regionKey);
}
