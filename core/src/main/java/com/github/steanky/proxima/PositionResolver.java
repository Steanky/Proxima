package com.github.steanky.proxima;

import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface PositionResolver {
    @NotNull Vec3I resolve(double x, double y, double z);

    default @NotNull Vec3I resolve(@NotNull Vec3D vec) {
        return resolve(vec.x(), vec.y(), vec.z());
    }

    PositionResolver FLOORED = Vec3I::immutableFloored;

    static @NotNull PositionResolver gravitating(@NotNull Space space, int seekHeight, double width, double epsilon) {
        Objects.requireNonNull(space);
        if (seekHeight <= 0) {
            throw new IllegalArgumentException("seekHeight must be positive");
        }

        double adjustedWidth = width - epsilon;
        if (!Double.isFinite(adjustedWidth)) {
            throw new IllegalArgumentException("width and epsilon must be finite");
        }

        if (adjustedWidth <= 0) {
            throw new IllegalArgumentException("width - epsilon must be positive");
        }

        double halfWidth = (width / 2) - (epsilon / 2);
        return (x, y, z) -> {
            int startX = (int) Math.floor(x);
            int endX = (int) Math.floor(x + adjustedWidth);

            int startY = (int) Math.floor(y);

            int startZ = (int) Math.floor(z);
            int endZ = (int) Math.floor(z + adjustedWidth);

            //if !evenHeight, we need to check the block region intersecting our feet
            boolean evenHeight = y == Math.rint(y);

            double ox = x - halfWidth;
            double oz = z - halfWidth;

            for (int i = evenHeight ? 0 : -1; i < seekHeight; i++) {
                int by = startY - (i + 1);

                Bounds3D highestBounds = null;

                double highestBoundsY = Double.NEGATIVE_INFINITY;
                int highestX = 0;
                int highestY = 0;
                int highestZ = 0;

                for (int bx = startX; bx <= endX; bx++) {
                    for (int bz = startZ; bz <= endZ; bz++) {
                        Solid solid = space.solidAt(bx, by, bz);

                        if (solid.isEmpty() || (i == -1 && solid.isFull())) {
                            continue;
                        }

                        if (solid.isFull()) {
                            return Vec3I.immutable(bx, by + 1, bz);
                        }

                        Bounds3D bounds = solid.closestCollision(bx, by, bz, ox, y, oz, adjustedWidth, 1,
                                adjustedWidth, Direction.DOWN, 2);

                        if (bounds != null) {
                            double boundsY = by + bounds.maxY();
                            if (boundsY > highestBoundsY) {
                                highestBounds = bounds;
                                highestBoundsY = boundsY;
                                highestX = bx;
                                highestY = by;
                                highestZ = bz;
                            }
                        }

                    }
                }

                if (highestBounds != null) {
                    return Vec3I.immutable(highestX, highestBounds.maxY() == 1 ? highestY + 1 : by, highestZ);
                }
            }

            return FLOORED.resolve(x, y, z);
        };
    }
}
