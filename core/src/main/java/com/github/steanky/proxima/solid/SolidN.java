package com.github.steanky.proxima.solid;

import com.github.steanky.toolkit.collection.Containers;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

final class SolidN implements Solid {
    private final List<Bounds3D> bounds;
    private final Bounds3D enclosing;

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
    public @NotNull @Unmodifiable List<Bounds3D> children() {
        return bounds;
    }

    @Override
    public int hashCode() {
        return bounds.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof Solid other) {
            return bounds.equals(other.children());
        }

        return false;
    }
}
