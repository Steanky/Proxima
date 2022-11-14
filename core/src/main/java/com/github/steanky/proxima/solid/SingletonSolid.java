package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;

public final class SingletonSolid extends AbstractSolid {
    private final Bounds3D bounds;
    private final boolean isFull;

    public SingletonSolid(@NotNull Bounds3D bounds) {
        this.bounds = bounds.immutable();
        this.isFull = this.bounds.originX() == 0 && this.bounds.originY() == 0 && this.bounds.originZ() == 0 &&
                this.bounds.lengthX() == 0 && this.bounds.lengthY() == 0 && this.bounds.lengthZ() == 0;
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
    public boolean overlaps(double ox, double oy, double oz, double lx, double ly, double lz) {
        return bounds.originX() < ox + lx && bounds.maxX() > ox && bounds.originY() < oy + ly && bounds.maxY() > oy &&
                bounds.originZ() < oz + lz && bounds.maxZ() > oz;
    }
}
