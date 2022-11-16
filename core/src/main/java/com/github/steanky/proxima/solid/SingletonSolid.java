package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class SingletonSolid implements Solid {
    private final Bounds3D bounds;
    private final List<Bounds3D> boundsList;
    private final boolean isFull;

    public SingletonSolid(@NotNull Bounds3D bounds) {
        this.bounds = bounds.immutable();
        this.boundsList = List.of(this.bounds);
        this.isFull = this.bounds.originX() == 0 && this.bounds.originY() == 0 && this.bounds.originZ() == 0 &&
                this.bounds.lengthX() == 1 && this.bounds.lengthY() == 1 && this.bounds.lengthZ() == 1;
    }

    @Override
    public @NotNull Bounds3D bounds() {
        return bounds;
    }

    @Override
    public boolean isFull() {
        return isFull;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public @NotNull @Unmodifiable List<Bounds3D> children() {
        return boundsList;
    }
}
