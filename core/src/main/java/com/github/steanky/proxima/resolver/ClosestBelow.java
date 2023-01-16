package com.github.steanky.proxima.resolver;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class ClosestBelow implements PositionResolver {
    private final Space space;
    private final int searchHeight;
    private final double width;
    private final double halfWidth;
    private final double epsilon;

    ClosestBelow(@NotNull Space space, int searchHeight, double width, double epsilon) {
        validate(searchHeight, width, epsilon);
        this.space = Objects.requireNonNull(space);
        this.searchHeight = searchHeight;
        this.width = width;
        this.halfWidth = width / 2;
        this.epsilon = epsilon;
    }

    private static void validate(int searchHeight, double width, double epsilon) {
        if (searchHeight < 0) {
            throw new IllegalArgumentException("searchHeight must be positive");
        }

        if (!Double.isFinite(width)) {
            throw new IllegalArgumentException("width must be finite");
        }

        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater than 0");
        }

        if (!Double.isFinite(epsilon)) {
            throw new IllegalArgumentException("epsilon must be finite");
        }

        if (epsilon < 0) {
            throw new IllegalArgumentException("epsilon must be non-negative");
        }

        if (epsilon > width) {
            throw new IllegalArgumentException("epsilon must not be larger than width");
        }
    }

    @Override
    public @NotNull Vec3I resolve(double x, double y, double z) {
        double ox = x - halfWidth;
        double oz = z - halfWidth;

        int sx = (int) Math.floor(x - halfWidth);
        int sy = (int) Math.floor(y);
        int sz = (int) Math.floor(z - halfWidth);

        int ex = (int) Math.floor(x + halfWidth);
        int ez = (int) Math.floor(z + halfWidth);

        for (int i = y == Math.floor(y) ? 0 : -1; i < searchHeight; i++) {
            int by = sy - (i + 1);

            double closestDistance = Double.POSITIVE_INFINITY;
            int cx = 0;
            int cy = 0;
            int cz = 0;

            for (int bx = sx; bx <= ex; bx++) {
                for (int bz = sz; bz <= ez; bz++) {
                    Solid solid = space.solidAt(bx, by, bz);

                    if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                        continue;
                    }

                    double thisDistance = Double.POSITIVE_INFINITY;
                    if (solid.isFull()) {
                        thisDistance = Vec3D.distanceSquared(bx + 0.5, by + 1, bz + 0.5, x, y, z);
                    } else {
                        Bounds3D closest =
                                solid.closestCollision(bx, by, bz, ox, y, oz, width, 1, width, Direction.DOWN,
                                        searchHeight, epsilon);
                        if (closest != null) {
                            thisDistance = Vec3D.distanceSquared(bx + 0.5, by + closest.maxY(), bz + 0.5, x, y, z);
                        }
                    }

                    if (thisDistance < closestDistance) {
                        closestDistance = thisDistance;
                        cx = bx;
                        cy = by;
                        cz = bz;
                    }
                }
            }

            if (Double.isFinite(closestDistance)) {
                return Vec3I.immutable(cx, cy, cz);
            }
        }

        return FLOORED.resolve(x, y, z);
    }
}