package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

public enum Direction {
    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0);

    public final int x;
    public final int y;
    public final int z;
    private final Vec3I vec;

    Direction(int x, int y, int z) {
        this.vec = Vec3I.immutable(x, y, z);

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public @NotNull Vec3I vector() {
        return vec;
    }
}
