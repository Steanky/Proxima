package com.github.steanky.proxima;

import com.github.steanky.proxima.path.PathResult;
import com.github.steanky.proxima.path.PathSettings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface Pathfinder {
    @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings);

    void shutdown();
}
