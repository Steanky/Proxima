package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

public enum Direction {
    UP(0, 1, 0, false),
    DOWN(0, -1, 0, false),
    NORTH(0, 0, -1, false),
    EAST(1, 0, 0, false),
    SOUTH(0, 0, 1, false),
    WEST(-1, 0, 0, false);

    private final Vec3I vec;

    public final int x;
    public final int y;
    public final int z;

    public final boolean intercardinal;

    Direction(int x, int y, int z, boolean intercardinal) {
        this.vec = Vec3I.immutable(x, y, z);

        this.x = x;
        this.y = y;
        this.z = z;

        this.intercardinal = intercardinal;
    }

    public @NotNull Vec3I vector() {
        return vec;
    }
}
