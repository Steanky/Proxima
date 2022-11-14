package com.github.steanky.proxima.solid;

import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSolid implements Solid {
    @Override
    public @Nullable Bounds3D expandOverlaps(double ox, double oy, double oz, double lx, double ly, double lz,
            double ex, double ey, double ez, @NotNull Solid.Order order) {
        lx += Math.abs(ex);
        ly += Math.abs(ey);
        lz += Math.abs(ez);

        if (ex < 0) {
            ox += ex;
        }

        if (ey < 0) {
            oy += ey;
        }

        if (ez < 0) {
            oz += ez;
        }

        return overlaps(ox, oy, oz, lx, ly, lz, order);
    }
}
