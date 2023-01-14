package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;

@FunctionalInterface
public interface Heuristic {
    Heuristic DISTANCE_SQUARED =
            (fromX, fromY, fromZ, toX, toY, toZ) -> (float) Vec3I.distanceSquared(fromX, fromY, fromZ, toX, toY, toZ);

    float heuristic(int fromX, int fromY, int fromZ, int toX, int toY, int toZ);
}
