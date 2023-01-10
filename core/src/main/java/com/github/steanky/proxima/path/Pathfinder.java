package com.github.steanky.proxima.path;

import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface Pathfinder {
    @NotNull Future<PathResult> pathfind(double x, double y, double z, int destX, int destY, int destZ,
            @NotNull PathSettings settings);

    default @NotNull Future<PathResult> pathfind(@NotNull Vec3D start, @NotNull Vec3I destination,
            @NotNull PathSettings settings) {
        return pathfind(start.x(), start.y(), start.z(), destination.x(), destination.y(), destination.z(),
                settings);
    }

    void shutdown();
}
