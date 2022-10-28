package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

public record NavigationNode(@NotNull Vec3I vectorPosition, double verticalOffset) {
}
