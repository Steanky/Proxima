package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

final class Solid12 implements Solid {
    private final Bounds3D bounds;
    private final List<Bounds3D> boundsList;
    private final boolean isFull;
    private final boolean hasChildren;

    Solid12(@NotNull Bounds3D first) {
        this.bounds = first.immutable();
        this.boundsList = List.of(this.bounds);
        this.isFull = this.bounds.volume() == 1;
        this.hasChildren = false;
    }

    Solid12(@NotNull Bounds3D first, @NotNull Bounds3D second) {
        this.bounds = Bounds3D.enclosingImmutable(first, second);
        this.boundsList = List.of(first.immutable(), second.immutable());
        this.isFull = false;
        this.hasChildren = true;
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
    public boolean hasChildren() {
        return hasChildren;
    }

    @Override
    public @NotNull @Unmodifiable List<Bounds3D> children() {
        return boundsList;
    }
}
