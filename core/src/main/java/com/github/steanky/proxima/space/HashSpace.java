package com.github.steanky.proxima.space;

import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.HashVec3I2ObjectMap;
import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

/**
 * A basic implementation of {@link Space}. Useful for testing. Not thread-safe; should not be used outside the context
 * of pre-constructed, unchanged environments.
 */
public class HashSpace implements Space {
    private final Vec3I2ObjectMap<Solid> solidMap;

    public HashSpace(int x, int y, int z, int lX, int lY, int lZ) {
        solidMap = new HashVec3I2ObjectMap<>(x, y, z, lX, lY, lZ);
    }

    public HashSpace(@NotNull Bounds3I bounds) {
        solidMap = new HashVec3I2ObjectMap<>(bounds);
    }

    @Override
    public @NotNull Solid solidAt(int x, int y, int z) {
        Solid mapped = solidMap.get(x, y, z);
        if (mapped == null) {
            return Solid.EMPTY;
        }

        return mapped;
    }

    public void remove(int x, int y, int z) {
        solidMap.remove(x, y, z);
    }

    public void put(int x, int y, int z, @NotNull Solid solid) {
        if (solid == Solid.EMPTY) {
            return;
        }

        solidMap.put(x, y, z, solid);
    }

    public void put(@NotNull Vec3I vec, @NotNull Solid solid) {
        put(vec.x(), vec.y(), vec.z(), solid);
    }
}
