package com.github.steanky.proxima.solid;

import com.github.steanky.toolkit.collection.Iterators;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class CompositeSolid implements Solid {
    private final List<Bounds3D> bounds;
    private final Bounds3D enclosing;
    private final boolean isFull;

    public CompositeSolid(@NotNull Bounds3D[] bounds) {
        if (bounds.length == 0) {
            throw new IllegalArgumentException("Cannot construct solid with no bounds");
        }

        Bounds3D[] newArray = new Bounds3D[bounds.length];
        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = bounds[i].immutable();
        }

        this.bounds = Iterators.arrayView(newArray);

        this.enclosing = Bounds3D.enclosingImmutable(newArray);
        this.isFull = newArray.length == 1 && enclosing.originX() == 0D && enclosing.originY() == 0D &&
                enclosing.originZ() == 0D && enclosing.lengthX() == 1D && enclosing.lengthY() == 1D &&
                enclosing.lengthZ() == 1D;
    }

    @Override
    public @NotNull Bounds3D bounds() {
        return enclosing;
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
        return bounds;
    }
}
