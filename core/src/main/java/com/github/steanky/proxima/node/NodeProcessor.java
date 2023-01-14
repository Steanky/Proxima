package com.github.steanky.proxima.node;

import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@FunctionalInterface
public interface NodeProcessor {
    void processPath(@NotNull Node head, @NotNull Vec3I2ObjectMap<Node> graph);

    NodeProcessor NO_CHANGE = (head, graph) -> {};

    static @NotNull NodeProcessor createDiagonals(@NotNull NodeSnapper nodeSnapper) {
        return new DiagonalProcessor(nodeSnapper);
    }

    static @NotNull NodeProcessor combined(@NotNull NodeProcessor @NotNull ... processors) {
        Objects.requireNonNull(processors);

        NodeProcessor[] copy = Arrays.copyOf(processors, processors.length);
        if (copy.length == 0) {
            return NO_CHANGE;
        }

        if (copy.length == 1) {
            return Objects.requireNonNull(copy[0], "processors array element");
        }

        for (int i = 0; i < processors.length; i++) {
            Objects.requireNonNull(copy[i], "processors array element");
        }

        return (head, graph) -> {
            for (NodeProcessor component : processors) {
                component.processPath(head, graph);
            }
        };
    }
}
