package com.github.steanky.proxima;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public record NavigationResult(@NotNull PathResult pathResult, @NotNull @Unmodifiable List<NavigationNode> nodes) {
}
