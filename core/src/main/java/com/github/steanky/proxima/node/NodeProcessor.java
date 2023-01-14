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
        if (processors.length == 0) {
            return NO_CHANGE;
        }

        NodeProcessor[] copy = Arrays.copyOf(processors, processors.length);
        if (copy.length == 1) {
            return Objects.requireNonNull(copy[0], "processors array element");
        }

        for (NodeProcessor processor : copy) {
            Objects.requireNonNull(processor, "processors array element");
        }

        return (head, graph) -> {
            for (NodeProcessor component : copy) {
                component.processPath(head, graph);
            }
        };
    }
}
