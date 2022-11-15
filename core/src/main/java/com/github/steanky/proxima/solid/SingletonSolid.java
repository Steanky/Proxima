package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SingletonSolid extends AbstractSolid {
    private final Bounds3D bounds;
    private final boolean isFull;

    public SingletonSolid(@NotNull Bounds3D bounds) {
        this.bounds = bounds.immutable();
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
    public @Nullable Bounds3D overlaps(double ox, double oy, double oz, double lx, double ly, double lz, @NotNull Solid.Order order) {
        if (bounds.originX() < ox + lx && bounds.maxX() > ox && bounds.originY() < oy + ly && bounds.maxY() > oy &&
                bounds.originZ() < oz + lz && bounds.maxZ() > oz) {
            return bounds;
        }

        return null;
    }
}
