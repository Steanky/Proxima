package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

public final class CompositeSolid extends AbstractSolid {
    private final Bounds3D[] bounds;
    private final Bounds3D enclosing;
    private final boolean isFull;

    public CompositeSolid(@NotNull Bounds3D[] bounds) {
        if (bounds.length == 0) {
            throw new IllegalArgumentException("Cannot construct solid with no bounds");
        }

        this.bounds = new Bounds3D[bounds.length];
        for (int i = 0; i < bounds.length; i++) {
            this.bounds[i] = bounds[i].immutable();
        }
        Arrays.sort(bounds, Comparator.comparing(Bounds3D::maxY));

        this.enclosing = Bounds3D.enclosingImmutable(bounds);
        this.isFull = enclosing.originX() == 0D && enclosing.originY() == 0D && enclosing.originZ() == 0D &&
                enclosing.lengthX() == 1D && enclosing.lengthY() == 1D && enclosing.lengthZ() == 1D;
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
    public @Nullable Bounds3D overlaps(double ox, double oy, double oz, double lx, double ly, double lz,
            @NotNull Solid.Order order) {
        switch (order) {
            case LOWEST:
                Bounds3D lowest = null;
                for (Bounds3D child : bounds) {
                    if (child.originX() < ox + lx && child.maxX() > ox && child.originY() < oy + ly &&
                            child.maxY() > oy && child.originZ() < oz + lz && child.maxZ() > oz) {
                        if (lowest == null || child.originY() < lowest.originY()) {
                            lowest = child;
                        }
                    }
                }

                return lowest;
            case NONE:
            case HIGHEST:
                for (int i = bounds.length - 1; i > -1; i--) {
                    Bounds3D child = bounds[i];
                    if (child.originX() < ox + lx && child.maxX() > ox && child.originY() < oy + ly &&
                            child.maxY() > oy && child.originZ() < oz + lz && child.maxZ() > oz) {
                        return child;
                    }
                }
                break;
        }

        return null;
    }
}
