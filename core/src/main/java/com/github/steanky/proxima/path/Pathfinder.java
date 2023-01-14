package com.github.steanky.proxima.path;

import com.github.steanky.vector.Vec3D;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface Pathfinder {
    @NotNull Future<PathResult> pathfind(double x, double y, double z, @NotNull PathTarget destination,
            @NotNull PathSettings settings);

    default @NotNull Future<PathResult> pathfind(@NotNull Vec3D start, @NotNull PathTarget destination,
            @NotNull PathSettings settings) {
        return pathfind(start.x(), start.y(), start.z(), destination, settings);
    }

    void shutdown();
}
