package com.github.steanky.proxima.node;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

public record NavigationNode(@NotNull Vec3I position, double verticalOffset) {
}