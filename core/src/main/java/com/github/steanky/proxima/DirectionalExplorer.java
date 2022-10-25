package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class DirectionalExplorer implements Explorer {
    private final Direction[] directions;
    private final DirectionalNodeSnapper nodeSnapper;

    public DirectionalExplorer(@NotNull Direction @NotNull [] directions, @NotNull DirectionalNodeSnapper nodeSnapper) {
        this.directions = Arrays.copyOf(directions, directions.length);
        this.nodeSnapper = Objects.requireNonNull(nodeSnapper);
    }

    @Override
    public void exploreEach(@NotNull Node currentNode, @NotNull NodeHandler handler) {
        for (Direction direction : directions) {
            Node parent = currentNode.parent;
            if (parent != null && nodeSnapper.canSkip(currentNode, parent, direction)) {
                continue;
            }

            nodeSnapper.snap(direction, currentNode, handler);
        }
    }
}
