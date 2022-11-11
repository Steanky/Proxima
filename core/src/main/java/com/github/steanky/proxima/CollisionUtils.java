package com.github.steanky.proxima;

public final class CollisionUtils {
    public static boolean overlaps(double ox1, double oz1, double lx1, double lz1,
            double ox2, double oz2, double lx2, double lz2) {
        double mx1 = ox1 + lx1;
        double mz1 = oz1 + lz1;

        double mx2 = ox2 + lx2;
        double mz2 = oz2 + lz2;

        return mx1 > ox2 && mz1 > oz2 && mx2 > ox1 && mz2 > ox1;
    }
}
