package com.github.steanky.proxima.path;

import com.github.steanky.proxima.node.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/**
 * The result of a pathfinding operation, representing a completed (successful or failed) path.
 */
public record PathResult(@NotNull @Unmodifiable List<Node> nodes, int exploredCount, boolean isSuccessful) {
    /**
     * Creates a new BasicResult.
     *
     * @param nodes       an unmodifiable list of nodes making up the path, from start to finish
     * @param exploredCount the number of nodes that were explored for this path, can be used to judge the computational
     *                      "difficulty" of this path
     * @param isSuccessful  if the path is successful (reached its destination)
     */
    public PathResult {
        Objects.requireNonNull(nodes, "vectors");
    }
}
