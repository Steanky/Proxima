package com.github.steanky.proxima.solid;

final class Util {
    static double checkDirection(double d, int b, double solidOrigin, double solidMax, double o, double m)  {
        if (d == 0) {
            return Double.NaN;
        }

        if (d < 0) {
            double diff = (b + solidMax) - o;
            if (diff < d || diff > 0) {
                return Double.NaN;
            }

            return Math.abs(diff);
        }

        double diff = (b + solidOrigin) - m;
        if (diff > d || diff < 0) {
            return Double.NaN;
        }

        return Math.abs(diff);
    }
}
