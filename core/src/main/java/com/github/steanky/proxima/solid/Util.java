package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.vector.Bounds3D;

/**
 * Collision checking utilities. Not part of the public API.
 */
final class Util {
    static Bounds3D closestCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, Direction d, double l) {
        if (solid.isEmpty()) {
            return null;
        }

        ox -= x;
        oy -= y;
        oz -= z;

        //fast directional expansion algorithm
        double adx = Math.abs(d.x);
        double ady = Math.abs(d.y);
        double adz = Math.abs(d.z);

        double dx = d.x * l;
        double dy = d.y * l;
        double dz = d.z * l;

        double aox = ox + Math.min(0, dx);
        double aoy = oy + Math.min(0, dy);
        double aoz = oz + Math.min(0, dz);

        double alx = lx + Math.abs(dx);
        double aly = ly + Math.abs(dy);
        double alz = lz + Math.abs(dz);

        double amx = aox + alx;
        double amy = aoy + aly;
        double amz = aoz + alz;

        double mx = ox + lx;
        double my = oy + ly;
        double mz = oz + lz;

        double closestDiff = Double.POSITIVE_INFINITY;
        Bounds3D closest = null;

        for (Bounds3D child : solid.children()) {
            if (!overlaps(child, aox, aoy, aoz, amx, amy, amz) || overlaps(child, ox, oy, oz, mx, my, mz)) {
                continue;
            }

            double diff = computeDiff(d, child, ox, oy, oz, mx, my, mz, adx, ady, adz);
            if (diff < closestDiff) {
                closestDiff = diff;
                closest = child;
            }
        }

        return closest;
    }

    static long minMaxCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, Direction d, double l) {
        if (solid.isEmpty()) {
            return Solid.NO_COLLISION;
        }

        ox -= x;
        oy -= y;
        oz -= z;

        double dx = d.x * l;
        double dy = d.y * l;
        double dz = d.z * l;

        double aox = ox + Math.min(0, dx);
        double aoy = oy + Math.min(0, dy);
        double aoz = oz + Math.min(0, dz);

        double alx = lx + Math.abs(dx);
        double aly = ly + Math.abs(dy);
        double alz = lz + Math.abs(dz);

        double amx = aox + alx;
        double amy = aoy + aly;
        double amz = aoz + alz;

        double mx = ox + lx;
        double my = oy + ly;
        double mz = oz + lz;

        float lowest = Float.POSITIVE_INFINITY;
        float highest = Float.NEGATIVE_INFINITY;

        for (Bounds3D child : solid.children()) {
            if (!overlaps(child, aox, aoy, aoz, amx, amy, amz) || overlaps(child, ox, oy, oz, mx, my, mz)) {
                continue;
            }

            float low = (float) child.originY();
            float high = (float) child.maxY();

            if (low < lowest) {
                lowest = low;
            }

            if (high > highest) {
                highest = high;
            }
        }

        return Solid.result(lowest, highest);
    }

    static boolean hasCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, Direction d, double l) {
        if (solid.isEmpty()) {
            return false;
        }

        ox -= x;
        oy -= y;
        oz -= z;

        double dx = d.x * l;
        double dy = d.y * l;
        double dz = d.z * l;

        double aox = ox + Math.min(0, dx);
        double aoy = oy + Math.min(0, dy);
        double aoz = oz + Math.min(0, dz);

        double alx = lx + Math.abs(dx);
        double aly = ly + Math.abs(dy);
        double alz = lz + Math.abs(dz);

        double amx = aox + alx;
        double amy = aoy + aly;
        double amz = aoz + alz;

        double mx = ox + lx;
        double my = oy + ly;
        double mz = oz + lz;

        for (Bounds3D child : solid.children()) {
            if (!overlaps(child, aox, aoy, aoz, amx, amy, amz) || overlaps(child, ox, oy, oz, mx, my, mz)) {
                continue;
            }

            return true;
        }

        return false;
    }

    private static double computeDiff(Direction d, Bounds3D child, double ox, double oy, double oz,
            double mx, double my, double mz, double adx, double ady, double adz) {
        return ((d.x < 0 ? ox - child.maxX() : child.maxX() - mx) * adx) +
                ((d.y < 0 ? oy - child.maxY() : child.maxY() - my) * ady) +
                ((d.z < 0 ? oz - child.maxZ() : child.maxZ() - mz) * adz);
    }

    private static boolean overlaps(Bounds3D bounds, double ox, double oy, double oz, double mx, double my, double mz) {
        return bounds.originX() < mx && bounds.originY() < my && bounds.originZ() < mz &&
                ox < bounds.maxX() && oy < bounds.maxY() && oz < bounds.maxZ();
    }
}