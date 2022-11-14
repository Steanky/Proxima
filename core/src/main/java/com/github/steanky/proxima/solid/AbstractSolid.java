package com.github.steanky.proxima.solid;

public abstract class AbstractSolid implements Solid {
    @Override
    public boolean expandOverlaps(double ox, double oy, double oz, double lx, double ly, double lz, double ex,
            double ey, double ez) {
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

        return overlaps(ox, oy, oz, lx, ly, lz);
    }
}
