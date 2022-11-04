package com.github.steanky.proxima.path;

import com.github.steanky.proxima.node.NavigationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public record NavigationResult(@NotNull PathResult pathResult, @NotNull @Unmodifiable List<NavigationNode> nodes) {
}
