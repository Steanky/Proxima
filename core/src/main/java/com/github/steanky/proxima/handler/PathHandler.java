package com.github.steanky.proxima.handler;

import com.github.steanky.proxima.PathResult;
import com.github.steanky.proxima.PathSettings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface PathHandler {
    @NotNull Future<PathResult> pathfind(int x, int y, int z, int destX, int destY, int destZ,
            @NotNull PathSettings settings);
}
