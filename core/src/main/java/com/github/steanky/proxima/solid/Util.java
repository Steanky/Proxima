package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.vector.Bounds3D;

/**
 * Default collision checking utilities. Not part of the public API.
 */
final class Util {
    static Bounds3D closestCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz, double lx, double ly, double lz, Direction d, double l, double e) {
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

        double eaox = aox + e;
        double eaoy = aoy + e;
        double eaoz = aoz + e;

        double eox = ox + e;
        double eoy = oy + e;
        double eoz = oz + e;

        double alx = lx + Math.abs(dx);
        double aly = ly + Math.abs(dy);
        double alz = lz + Math.abs(dz);

        double amx = aox + alx - e;
        double amy = aoy + aly - e;
        double amz = aoz + alz - e;

        double mx = ox + lx - e;
        double my = oy + ly - e;
        double mz = oz + lz - e;

        double closestDiff = Double.POSITIVE_INFINITY;
        Bounds3D closest = null;

        for (Bounds3D child : solid.children()) {
            if (!overlaps(child, x, y, z, eaox, eaoy, eaoz, amx, amy, amz) || overlaps(child, x, y, z, eox, eoy, eoz, mx, my, mz)) {
                continue;
            }

            double diff = computeDiff(d, child, eox, eoy, eoz, mx, my, mz, adx, ady, adz);
            if (diff < closestDiff) {
                closestDiff = diff;
                closest = child;
            }
        }

        return closest;
    }

    static long minMaxCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz, double lx, double ly, double lz, Direction d, double l, double e) {
        double dx = d.x * l;
        double dy = d.y * l;
        double dz = d.z * l;

        double aox = ox + Math.min(0, dx);
        double aoy = oy + Math.min(0, dy);
        double aoz = oz + Math.min(0, dz);

        double eaox = aox + e;
        double eaoy = aoy + e;
        double eaoz = aoz + e;

        double eox = ox + e;
        double eoy = oy + e;
        double eoz = oz + e;

        double alx = lx + Math.abs(dx);
        double aly = ly + Math.abs(dy);
        double alz = lz + Math.abs(dz);

        double amx = aox + alx - e;
        double amy = aoy + aly - e;
        double amz = aoz + alz - e;

        double mx = ox + lx - e;
        double my = oy + ly - e;
        double mz = oz + lz - e;

        float lowest = Float.POSITIVE_INFINITY;
        float highest = Float.NEGATIVE_INFINITY;

        for (Bounds3D child : solid.children()) {
            if (!overlaps(child, x, y, z, eaox, eaoy, eaoz, amx, amy, amz) || overlaps(child, x, y, z, eox, eoy, eoz, mx, my, mz)) {
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

    static boolean hasCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz, double lx, double ly, double lz, Direction d, double l, double e) {
        double dx = d.x * l;
        double dy = d.y * l;
        double dz = d.z * l;

        double aox = ox + Math.min(0, dx);
        double aoy = oy + Math.min(0, dy);
        double aoz = oz + Math.min(0, dz);

        double eaox = aox + e;
        double eaoy = aoy + e;
        double eaoz = aoz + e;

        double eox = ox + e;
        double eoy = oy + e;
        double eoz = oz + e;

        double alx = lx + Math.abs(dx);
        double aly = ly + Math.abs(dy);
        double alz = lz + Math.abs(dz);

        double amx = aox + alx - e;
        double amy = aoy + aly - e;
        double amz = aoz + alz - e;

        double mx = ox + lx - e;
        double my = oy + ly - e;
        double mz = oz + lz - e;

        for (Bounds3D child : solid.children()) {
            if (!overlaps(child, x, y, z, eaox, eaoy, eaoz, amx, amy, amz) || overlaps(child, x, y, z, eox, eoy, eoz, mx, my, mz)) {
                continue;
            }

            return true;
        }

        return false;
    }

    static boolean hasCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz, double lx, double ly, double lz, double dx, double dy, double dz, double e) {
        double adx = Math.abs(dx);
        double ady = Math.abs(dy);
        double adz = Math.abs(dz);

        double elx = lx - e;
        double ely = ly - e;
        double elz = lz - e;

        double adjustedXZ = (elz * adx + elx * adz) / 2;
        double adjustedXY = (ely * adx + elx * ady) / 2;
        double adjustedYZ = (elz * ady + ely * adz) / 2;

        double cx = ox + (lx / 2);
        double cy = oy + (ly / 2);
        double cz = oz + (lz / 2);

        double mx = ox + lx - e;
        double my = oy + ly - e;
        double mz = oz + lz - e;

        double eox = ox + e;
        double eoy = oy + e;
        double eoz = oz + e;

        double exox = eox + Math.min(0, dx);
        double exoy = eoy + Math.min(0, dy);
        double exoz = eoz + Math.min(0, dz);

        double exmx = Math.min(0, dx) + adx + mx;
        double exmy = Math.min(0, dy) + ady + my;
        double exmz = Math.min(0, dz) + adz + mz;

        for (Bounds3D child : solid.children()) {
            if (overlaps(child, x, y, z, eox, eoy, eoz, mx, my, mz) || !overlaps(child, x, y, z, exox, exoy, exoz, exmx, exmy, exmz)) {
                continue;
            }

            if (checkBounds(x, y, z, child, cx, cy, cz, adjustedXZ, adjustedXY, adjustedYZ, dx, dy, dz)) {
                return true;
            }
        }

        return false;
    }

    static long minMaxCollision(Solid solid, int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, double dx, double dy, double dz, double e) {
        double adx = Math.abs(dx);
        double ady = Math.abs(dy);
        double adz = Math.abs(dz);

        double elx = lx - e;
        double ely = ly - e;
        double elz = lz - e;

        double adjustedXZ = (elz * adx + elx * adz) / 2;
        double adjustedXY = (ely * adx + elx * ady) / 2;
        double adjustedYZ = (elz * ady + ely * adz) / 2;

        double mx = cx + (lx / 2) - e;
        double my = cy + ly - e;
        double mz = cz + (lz / 2) - e;

        double eox = cx - (lx / 2) + e;
        double eoy = cy + e;
        double eoz = cz - (lz / 2) + e;

        double exox = eox + Math.min(0, dx);
        double exoy = eoy + Math.min(0, dy);
        double exoz = eoz + Math.min(0, dz);

        double exmx = Math.min(0, dx) + adx + mx;
        double exmy = Math.min(0, dy) + ady + my;
        double exmz = Math.min(0, dz) + adz + mz;

        float lowest = Float.POSITIVE_INFINITY;
        float highest = Float.NEGATIVE_INFINITY;

        for (Bounds3D child : solid.children()) {
            if (overlaps(child, x, y, z, eox, eoy, eoz, mx, my, mz) || !overlaps(child, x, y, z, exox, exoy, exoz, exmx, exmy, exmz)) {
                continue;
            }

            if (!checkBounds(x, y, z, child, cx, cy, cz, adjustedXZ, adjustedXY, adjustedYZ, dx, dy, dz)) {
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

    private static boolean checkBounds(int x, int y, int z, Bounds3D component, double cx, double cy, double cz, double adjustedXZ, double adjustedXY, double adjustedYZ, double dX, double dY, double dZ) {
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

    private static boolean checkAxis(double size, double dA, double dB, double minA, double minB, double maxA, double maxB) {
        if (dA == 0 && dB == 0) {
            return true;
        }

        return dA * dB <= 0 ? checkPlanes(size, dA, dB, minA, minB, maxA, maxB) :
                checkPlanes(size, dA, dB, maxA, minB, minA, maxB);
    }

    private static boolean checkPlanes(double size, double dA, double dB, double minA, double minB, double maxA, double maxB) {
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

    private static double computeDiff(Direction d, Bounds3D child, double ox, double oy, double oz, double mx, double my, double mz, double adx, double ady, double adz) {
        return ((d.x < 0 ? ox - child.maxX() : child.maxX() - mx) * adx) +
                ((d.y < 0 ? oy - child.maxY() : child.maxY() - my) * ady) +
                ((d.z < 0 ? oz - child.maxZ() : child.maxZ() - mz) * adz);
    }

    private static boolean overlaps(Bounds3D bounds, int x, int y, int z, double ox, double oy, double oz, double mx, double my, double mz) {
        return bounds.originX() + x < mx && bounds.originY() + y < my && bounds.originZ() + z < mz && ox < bounds.maxX() + x &&
                oy < bounds.maxY() + y && oz < bounds.maxZ() + z;
    }
}