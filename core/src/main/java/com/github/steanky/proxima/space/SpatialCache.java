package com.github.steanky.proxima.space;

import com.github.steanky.vector.Vec3IFunction;
import org.jetbrains.annotations.NotNull;

public interface SpatialCache<T> {
    T get(int x, int y, int z);

    void put(int x, int y, int z, @NotNull T item);

    @NotNull T computeIfAbsent(int x, int y, int z, @NotNull Vec3IFunction<T> supplier);

    void remove(int x, int y, int z);

    void clear();
}
