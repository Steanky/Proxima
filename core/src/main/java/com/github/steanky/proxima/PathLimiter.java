package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.Vec3D;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface PathLimiter {
    PathLimiter NO_LIMIT = (currentNode) -> true;

    static @NotNull PathLimiter pathLength(float maxLength) {
        return currentNode -> currentNode.g < maxLength;
    }

    static @NotNull PathLimiter inBounds(@NotNull Bounds3I bounds) {
        Objects.requireNonNull(bounds);
        return n -> bounds.contains(n.x, n.y, n.z);
    }

    static @NotNull PathLimiter inRadius(@NotNull Vec3D origin, double r) {
        double radiusSquared = r * r;
        return n -> Vec3D.distanceSquared(origin.x(), origin.y(), origin.z(), n.x, n.y, n.z) < radiusSquared;
    }

    boolean inBounds(@NotNull Node currentNode);

    default @NotNull PathLimiter not() {
        return currentNode -> !PathLimiter.this.inBounds(currentNode);
    }

    default @NotNull PathLimiter and(@NotNull PathLimiter other) {
        Objects.requireNonNull(other);
        return currentNode -> PathLimiter.this.inBounds(currentNode) && other.inBounds(currentNode);
    }

    default @NotNull PathLimiter or(@NotNull PathLimiter other) {
        Objects.requireNonNull(other);
        return currentNode -> PathLimiter.this.inBounds(currentNode) || other.inBounds(currentNode);
    }

    default @NotNull PathLimiter xor(@NotNull PathLimiter other) {
        Objects.requireNonNull(other);
        return currentNode -> PathLimiter.this.inBounds(currentNode) ^ other.inBounds(currentNode);
    }
}
