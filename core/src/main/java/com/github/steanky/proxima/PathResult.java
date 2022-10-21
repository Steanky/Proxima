package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/**
 * The result of a pathfinding operation, representing a completed (successful or failed) path.
 */
public record PathResult(@NotNull @Unmodifiable List<Vec3I> vectors, int exploredCount, boolean isSuccessful) {
    /**
     * Creates a new BasicResult.
     *
     * @param getStart      the starting node
     * @param exploredCount the number of nodes that were explored for this path
     * @param isSuccessful  if the path is successful
     */
    public PathResult {
        Objects.requireNonNull(vectors, "vectors");
    }
}
