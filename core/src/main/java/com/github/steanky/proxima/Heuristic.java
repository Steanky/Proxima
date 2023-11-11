package com.github.steanky.proxima;

import com.github.steanky.vector.Vec3I;

public interface Heuristic {
    double SQRT_2 = Math.sqrt(2);
    double SQRT_3 = Math.sqrt(3);

    Heuristic ZERO = new Heuristic() {
        @Override
        public float heuristic(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
            return 0;
        }

        @Override
        public double scale() {
            return 0;
        }
    };

    Heuristic DISTANCE = new Heuristic() {
        @Override
        public float heuristic(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
            return (float) Vec3I.distance(fromX, fromY, fromZ, toX, toY, toZ);
        }

        @Override
        public double scale() {
            return 1;
        }
    };

    Heuristic DISTANCE_SQUARED = new Heuristic() {
        @Override
        public float heuristic(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
            return (float) Vec3I.distanceSquared(fromX, fromY, fromZ, toX, toY, toZ);
        }

        @Override
        public double scale() {
            return 1;
        }
    };

    Heuristic OCTILE = new Heuristic() {
        @Override
        public float heuristic(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
            int dx = Math.abs(toX - fromX);
            int dy = Math.abs(toY - fromY);
            int dz = Math.abs(toZ - fromZ);

            int dmin = Math.min(dx, Math.min(dy, dz));
            int dmax = Math.max(dx, Math.max(dy, dz));

            int dmid = dx + dy + dz - dmin - dmax;

            return (float) ((SQRT_3 - SQRT_2) * dmin + (SQRT_2 - 1) * dmid + dmax);
        }

        @Override
        public double scale() {
            return 1.4143;
        }
    };

    float heuristic(int fromX, int fromY, int fromZ, int toX, int toY, int toZ);

    double scale();
}
