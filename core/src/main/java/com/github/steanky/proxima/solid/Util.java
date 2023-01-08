package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.vector.Bounds3D;

final class Util {
    static Bounds3D closestCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, Direction d, double l) {
        if (solid.isEmpty()) {
            return null;
        }

        int c1;
        int c2;

        double ao1;
        double am1;

        double ao2;
        double am2;

        double direction;
        int coordinate;
        double agentOrigin;
        double agentMax;

        switch (d) {
            case EAST, WEST -> {
                c1 = y;
                c2 = z;

                ao1 = oy;
                am1 = oy + ly;

                ao2 = oz;
                am2 = oz + lz;

                direction = d.x * l;
                coordinate = x;
                agentOrigin = ox;
                agentMax = ox + lx;
            }
            case UP, DOWN -> {
                c1 = x;
                c2 = z;

                ao1 = ox;
                am1 = ox + lx;

                ao2 = oz;
                am2 = oz + lz;

                direction = d.y * l;
                coordinate = y;
                agentOrigin = oy;
                agentMax = oy + ly;
            }
            case NORTH, SOUTH -> {
                c1 = x;
                c2 = y;

                ao1 = ox;
                am1 = ox + lx;

                ao2 = oy;
                am2 = oy + ly;

                direction = d.z * l;
                coordinate = z;
                agentOrigin = oz;
                agentMax = oz + lz;
            }
            default -> throw new IllegalStateException("Unexpected direction: " + d);
        }

        Bounds3D closest = null;
        double lastDiff = Double.POSITIVE_INFINITY;

        for (Bounds3D child : solid.children()) {
            double solidOrigin;
            double solidMax;

            double so1;
            double sm1;

            double so2;
            double sm2;

            switch (d) {
                case EAST, WEST -> {
                    solidOrigin = child.originX();
                    solidMax = child.maxX();

                    so1 = child.originY();
                    sm1 = child.maxY();

                    so2 = child.originZ();
                    sm2 = child.maxZ();
                }
                case UP, DOWN -> {
                    solidOrigin = child.originY();
                    solidMax = child.maxY();

                    so1 = child.originX();
                    sm1 = child.maxX();

                    so2 = child.originZ();
                    sm2 = child.maxZ();
                }
                case NORTH, SOUTH -> {
                    solidOrigin = child.originZ();
                    solidMax = child.maxZ();

                    so1 = child.originX();
                    sm1 = child.maxX();

                    so2 = child.originY();
                    sm2 = child.maxY();
                }
                default -> throw new IllegalStateException("Unexpected direction: " + d);
            }

            if (Util.doubleAxisIntersect(c1, c2, so1, sm1, so2, sm2, ao1, am1, ao2, am2)) {
                double diff = Util.checkDirection(direction, coordinate, solidOrigin, solidMax, agentOrigin, agentMax);
                if (diff < lastDiff) {
                    closest = child;
                    lastDiff = diff;
                }
            }
        }

        return closest;
    }

    static long minMaxCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, Direction d, double l) {
        if (solid.isEmpty()) {
            return Solid.NO_COLLISION;
        }

        int c1;
        int c2;

        double ao1;
        double am1;

        double ao2;
        double am2;

        double direction;
        int coordinate;
        double agentOrigin;
        double agentMax;

        switch (d) {
            case EAST, WEST -> {
                c1 = y;
                c2 = z;

                ao1 = oy;
                am1 = oy + ly;

                ao2 = oz;
                am2 = oz + lz;

                direction = d.x * l;
                coordinate = x;
                agentOrigin = ox;
                agentMax = ox + lx;
            }
            case UP, DOWN -> {
                c1 = x;
                c2 = z;

                ao1 = ox;
                am1 = ox + lx;

                ao2 = oz;
                am2 = oz + lz;

                direction = d.y * l;
                coordinate = y;
                agentOrigin = oy;
                agentMax = oy + ly;
            }
            case NORTH, SOUTH -> {
                c1 = x;
                c2 = y;

                ao1 = ox;
                am1 = ox + lx;

                ao2 = oy;
                am2 = oy + ly;

                direction = d.z * l;
                coordinate = z;
                agentOrigin = oz;
                agentMax = oz + lz;
            }
            default -> throw new IllegalStateException("Unexpected direction: " + d);
        }

        float lowest = Float.POSITIVE_INFINITY;
        float highest = Float.NEGATIVE_INFINITY;

        for (Bounds3D child : solid.children()) {
            double solidOrigin;
            double solidMax;

            double so1;
            double sm1;

            double so2;
            double sm2;

            switch (d) {
                case EAST, WEST -> {
                    solidOrigin = child.originX();
                    solidMax = child.maxX();

                    so1 = child.originY();
                    sm1 = child.maxY();

                    so2 = child.originZ();
                    sm2 = child.maxZ();
                }
                case UP, DOWN -> {
                    solidOrigin = child.originY();
                    solidMax = child.maxY();

                    so1 = child.originX();
                    sm1 = child.maxX();

                    so2 = child.originZ();
                    sm2 = child.maxZ();
                }
                case NORTH, SOUTH -> {
                    solidOrigin = child.originZ();
                    solidMax = child.maxZ();

                    so1 = child.originX();
                    sm1 = child.maxX();

                    so2 = child.originY();
                    sm2 = child.maxY();
                }
                default -> throw new IllegalStateException("Unexpected direction: " + d);
            }

            if (Util.doubleAxisIntersect(c1, c2, so1, sm1, so2, sm2, ao1, am1, ao2, am2)) {
                double diff = Util.checkDirection(direction, coordinate, solidOrigin, solidMax, agentOrigin, agentMax);
                if (!Double.isNaN(diff)) {
                    float low = (float) child.originY();
                    float high = (float) child.maxY();

                    if (low < lowest) {
                        lowest = low;
                    }

                    if (high > highest) {
                        highest = high;
                    }
                }
            }
        }

        return Solid.result(lowest, highest);
    }

    static boolean hasCollision(Solid solid, int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, Direction d, double l) {
        if (solid.isEmpty()) {
            return false;
        }

        int c1;
        int c2;

        double ao1;
        double am1;

        double ao2;
        double am2;

        double direction;
        int coordinate;
        double agentOrigin;
        double agentMax;

        switch (d) {
            case EAST, WEST -> {
                c1 = y;
                c2 = z;

                ao1 = oy;
                am1 = oy + ly;

                ao2 = oz;
                am2 = oz + lz;

                direction = d.x * l;
                coordinate = x;
                agentOrigin = ox;
                agentMax = ox + lx;
            }
            case UP, DOWN -> {
                c1 = x;
                c2 = z;

                ao1 = ox;
                am1 = ox + lx;

                ao2 = oz;
                am2 = oz + lz;

                direction = d.y * l;
                coordinate = y;
                agentOrigin = oy;
                agentMax = oy + ly;
            }
            case NORTH, SOUTH -> {
                c1 = x;
                c2 = y;

                ao1 = ox;
                am1 = ox + lx;

                ao2 = oy;
                am2 = oy + ly;

                direction = d.z * l;
                coordinate = z;
                agentOrigin = oz;
                agentMax = oz + lz;
            }
            default -> throw new IllegalStateException("Unexpected direction: " + d);
        }

        for (Bounds3D child : solid.children()) {
            double solidOrigin;
            double solidMax;

            double so1;
            double sm1;

            double so2;
            double sm2;

            switch (d) {
                case EAST, WEST -> {
                    solidOrigin = child.originX();
                    solidMax = child.maxX();

                    so1 = child.originY();
                    sm1 = child.maxY();

                    so2 = child.originZ();
                    sm2 = child.maxZ();
                }
                case UP, DOWN -> {
                    solidOrigin = child.originY();
                    solidMax = child.maxY();

                    so1 = child.originX();
                    sm1 = child.maxX();

                    so2 = child.originZ();
                    sm2 = child.maxZ();
                }
                case NORTH, SOUTH -> {
                    solidOrigin = child.originZ();
                    solidMax = child.maxZ();

                    so1 = child.originX();
                    sm1 = child.maxX();

                    so2 = child.originY();
                    sm2 = child.maxY();
                }
                default -> throw new IllegalStateException("Unexpected direction: " + d);
            }

            if (Util.doubleAxisIntersect(c1, c2, so1, sm1, so2, sm2, ao1, am1, ao2, am2)) {
                double diff = Util.checkDirection(direction, coordinate, solidOrigin, solidMax, agentOrigin, agentMax);
                if (!Double.isNaN(diff)) {
                    return true;
                }
            }
        }

        return false;
    }

    static double checkDirection(double direction, int coordinate, double solidOrigin, double solidMax,
            double agentOrigin, double agentMax)  {
        if (direction == 0) {
            return Double.NaN;
        }

        double diff;
        if (direction < 0) {
            diff = (coordinate + solidMax) - agentOrigin;
            if (diff < direction || diff > 0) {
                return Double.NaN;
            }
        }
        else {
            diff = (coordinate + solidOrigin) - agentMax;
            if (diff > direction || diff < 0) {
                return Double.NaN;
            }
        }

        return Math.abs(diff);
    }

    public static boolean doubleAxisIntersect(
            int c1, int c2,
            double so1, double sm1, double so2, double sm2,
            double ao1, double am1, double ao2, double am2) {
        return c1 + so1 < am1 && c1 + sm1 > ao1 && c2 + so2 < am2 && c2 + sm2 > ao2;
    }
}