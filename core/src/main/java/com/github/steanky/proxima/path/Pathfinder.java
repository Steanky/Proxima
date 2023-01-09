package com.github.steanky.proxima.path;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface Pathfinder {
    @NotNull Future<PathResult> pathfind(int x, int y, int z, float yOffset, int destX, int destY, int destZ,
            @NotNull PathSettings settings);

    default @NotNull Future<PathResult> pathfind(@NotNull Vec3I start, float yOffset, @NotNull Vec3I destination,
            @NotNull PathSettings settings) {
        return pathfind(start.x(), start.y(), start.z(), yOffset, destination.x(), destination.y(), destination.z(),
                settings);
    }

    void shutdown();
}
