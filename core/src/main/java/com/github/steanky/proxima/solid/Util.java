package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.vector.Bounds3D;

/**
 * Default collision checking utilities. Not part of the public API.
 */
final class Util {
    static Bounds3D closestCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, Direction d, double l) {
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

    static boolean hasCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, double dx, double dy, double dz) {
        double adx = Math.abs(dx);
        double ady = Math.abs(dy);
        double adz = Math.abs(dz);

        double adjustedXZ = (lz * adx + lx * adz) / 2;
        double adjustedXY = (ly * adx + lx * ady) / 2;
        double adjustedYZ = (lz * ady + ly * adz) / 2;

        double cx = ox + (lx / 2);
        double cy = oy + (ly / 2);
        double cz = oz + (lz / 2);

        ox -= x;
        oy -= y;
        oz -= z;

        double mx = ox + lx;
        double my = oy + ly;
        double mz = oz + lz;

        for (Bounds3D child : solid.children()) {
            if (overlaps(child, ox, oy, oz, mx, my, mz)) {
                continue;
            }

            if (checkBounds(x, y, z, child, cx, cy, cz, adjustedXZ, adjustedXY, adjustedYZ, dx, dy, dz)) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkBounds(int x, int y, int z, Bounds3D component, double cx, double cy,
            double cz, double adjustedXZ, double adjustedXY, double adjustedYZ, double dX, double dY, double dZ) {
        double minX = x + component.originX() - cx;
        double minY = y + component.originY() - cy;
        double minZ = z + component.originZ() - cz;

        double maxX = x + component.maxX() - cx;
        double maxY = y + component.maxY() - cy;
        double maxZ = z + component.maxZ() - cz;

        return checkAxis(adjustedXZ, dX, dZ, minX, minZ, maxX, maxZ) &&
                checkAxis(adjustedXY, dX, dY, minX, minY, maxX, maxY) &&
                checkAxis(adjustedYZ, dZ, dY, minZ, minY, maxZ, maxY);
    }

    private static boolean checkAxis(double size, double dA, double dB, double minA, double minB, double maxA,
            double maxB) {
        if (dA == 0 && dB == 0) {
            return true;
        }

        return dA * dB <= 0 ? checkPlanes(size, dA, dB, minA, minB, maxA, maxB) :
                checkPlanes(size, dA, dB, maxA, minB, minA, maxB);
    }

    private static boolean checkPlanes(double size, double dA, double dB, double minA, double minB, double maxA,
            double maxB) {
        double bMinusAMin = (minB * dA) - (minA * dB);
        if (bMinusAMin >= size) { //!minInFirst
            return (maxB * dA) - (maxA * dB) < size;  //... && maxInFirst
        }

        //we know minInFirst is true
        if (bMinusAMin > -size) { //... && minInSecond
            return true;
        }

        return (maxB * dA) - (maxA * dB) > -size; // ... && !minInSecond
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