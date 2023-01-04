package com.github.steanky.proxima.solid;

import com.github.steanky.toolkit.collection.Containers;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

final class SolidN implements Solid {
    private final List<Bounds3D> bounds;
    private final Bounds3D enclosing;
    private final boolean hasChildren;

    SolidN(@NotNull Bounds3D @NotNull [] bounds) {
        if (bounds.length == 0) {
            throw new IllegalArgumentException("Cannot construct solid with no bounds");
        }

        Bounds3D[] newArray = new Bounds3D[bounds.length];
        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = bounds[i].immutable();
        }

        this.bounds = Containers.arrayView(newArray);
        this.enclosing = Bounds3D.enclosingImmutable(newArray);
        this.hasChildren = newArray.length > 1;
    }

    @Override
    public @NotNull Bounds3D bounds() {
        return enclosing;
    }

    @Override
    public boolean isFull() {
        return false;
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
        return bounds;
    }
}
