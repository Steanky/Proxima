package com.github.steanky.proxima.path;

import com.github.steanky.proxima.node.Node;
import org.jetbrains.annotations.Nullable;

/**
 * The result of a pathfinding operation, representing a completed (successful or failed) path.
 */
public record PathResult(@Nullable Node head, int exploredCount, boolean isSuccessful) {
    /**
     * The empty, unsuccessful path result.
     */
    public static PathResult EMPTY = new PathResult(null, 0, false);

    /**
     * Creates a new PathResult.
     *
     * @param exploredCount the number of nodes that were explored for this path, can be used to judge the computational
     *                      "difficulty" of this path
     * @param isSuccessful  if the path is successful (reached its destination)
     */
    public PathResult {}
}
