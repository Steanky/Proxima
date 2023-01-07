package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface Solid {
    Solid EMPTY = new Solid() {
        @Override
        public @NotNull Bounds3D bounds() {
            throw new IllegalStateException("No bounds for empty solids");
        }

        @Override
        public boolean isFull() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public @NotNull @Unmodifiable List<Bounds3D> children() {
            return List.of();
        }
    };

    Solid FULL = new Solid12(Bounds3D.immutable(0, 0, 0, 1, 1, 1));

    @NotNull Bounds3D bounds();

    boolean isFull();

    boolean isEmpty();

    @NotNull @Unmodifiable List<Bounds3D> children();

    static long result(float lowest, float highest) {
        return (((long) Float.floatToRawIntBits(lowest)) << 32) | Float.floatToRawIntBits(highest);
    }

    static float lowest(long collisionResult) {
        return Float.intBitsToFloat((int) (collisionResult >>> 32));
    }

    static float highest(long collisionResult) {
        return Float.intBitsToFloat((int) collisionResult);
    }

    /**
     * Returned by {@link Solid#minMaxCollision(int, int, int, double, double, double, double, double, double,
     * Direction, double)} to indicate no collision was found.
     */
    long NO_COLLISION = 0x7F800000_FF800000L;

    default long minMaxCollision(int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, @NotNull Direction direction, double l) {
        if (isEmpty()) {
            return NO_COLLISION;
        }

        float lowest = Float.POSITIVE_INFINITY;
        float highest = Float.NEGATIVE_INFINITY;

        switch (direction) {
            case EAST, WEST -> {
                double dx = direction.x * l;
                double mx = ox + lx;

                for (Bounds3D child : children()) {
                    if (!Double.isNaN(Util.checkDirection(dx, x, child.originX(), child.maxX(), ox, mx))) {
                        double min = child.originY();
                        if (min < lowest) {
                            lowest = (float) min;
                        }

                        double max = child.maxY();
                        if (max > highest) {
                            highest = (float) max;
                        }
                    }
                }
            }
            case UP, DOWN -> {
                double dy = direction.y * l;
                double my = oy + ly;

                for (Bounds3D child : children()) {
                    if (!Double.isNaN(Util.checkDirection(dy, y, child.originY(), child.maxY(), oy, my))) {
                        double min = child.originY();
                        if (min < lowest) {
                            lowest = (float) min;
                        }

                        double max = child.maxY();
                        if (max > highest) {
                            highest = (float) max;
                        }
                    }
                }
            }
            case NORTH, SOUTH -> {
                double dz = direction.z * l;
                double mz = oz + lz;

                for (Bounds3D child : children()) {
                    if (!Double.isNaN(Util.checkDirection(dz, z, child.originZ(), child.maxZ(), oz, mz))) {
                        double min = child.originY();
                        if (min < lowest) {
                            lowest = (float) min;
                        }

                        double max = child.maxY();
                        if (max > highest) {
                            highest = (float) max;
                        }
                    }
                }
            }
        }

        return result(lowest, highest);
    }

    default @Nullable Bounds3D closestCollision(int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, @NotNull Direction direction, double l) {
        if (isEmpty()) {
            return null;
        }

        Bounds3D closest = null;
        double lastDiff = Double.POSITIVE_INFINITY;

        return switch (direction) {
            case EAST, WEST -> {
                double dx = direction.x * l;
                double mx = ox + lx;

                for (Bounds3D child : children()) {
                    double diffX = Util.checkDirection(dx, x, child.originX(), child.maxX(), ox, mx);
                    if (!Double.isNaN(diffX)) {
                        if (diffX < lastDiff) {
                            closest = child;
                        }
                    }
                }

                yield closest;
            }
            case UP, DOWN -> {
                double dy = direction.y * l;
                double my = oy + ly;

                for (Bounds3D child : children()) {
                    double diffY = Util.checkDirection(dy, y, child.originY(), child.maxY(), oy, my);
                    if (!Double.isNaN(diffY)) {
                        if (diffY < lastDiff) {
                            closest = child;
                        }
                    }
                }

                yield closest;
            }
            case NORTH, SOUTH -> {
                double dz = direction.z * l;
                double mz = oz + lz;

                for (Bounds3D child : children()) {
                    double diffZ = Util.checkDirection(dz, z, child.originZ(), child.maxZ(), oz, mz);
                    if (!Double.isNaN(diffZ)) {
                        if (diffZ < lastDiff) {
                            closest = child;
                        }
                    }
                }

                yield closest;
            }
        };
    }

    default boolean hasCollision(int x, int y, int z, double ox, double oy, double oz,
            double lx, double ly, double lz, @NotNull Direction direction, double l) {
        if (isEmpty()) {
            return false;
        }

        return switch (direction) {
            case EAST, WEST -> {
                double dx = direction.x * l;
                double mx = ox + lx;

                for (Bounds3D child : children()) {
                    if (!Double.isNaN(Util.checkDirection(dx, x, child.originX(), child.maxX(), ox, mx))) {
                        yield true;
                    }
                }

                yield false;
            }
            case UP, DOWN -> {
                double dy = direction.y * l;
                double my = oy + ly;

                for (Bounds3D child : children()) {
                    if (!Double.isNaN(Util.checkDirection(dy, y, child.originY(), child.maxY(), oy, my))) {
                        yield true;
                    }
                }

                yield false;
            }
            case NORTH, SOUTH -> {
                double dz = direction.z * l;
                double mz = oz + lz;

                for (Bounds3D child : children()) {
                    if (!Double.isNaN(Util.checkDirection(dz, z, child.originZ(), child.maxZ(), oz, mz))) {
                        yield true;
                    }
                }

                yield false;
            }
        };
    }

    static @NotNull Solid of(@NotNull Bounds3D bounds) {
        return new Solid12(bounds);
    }

    static @NotNull Solid of(@NotNull Bounds3D first, @NotNull Bounds3D second) {
        return new Solid12(first, second);
    }

    static @NotNull Solid of(@NotNull Bounds3D @NotNull ... bounds) {
        if (bounds.length == 0) {
            return EMPTY;
        }

        if (bounds.length == 1) {
            return new Solid12(bounds[0]);
        }

        if (bounds.length == 2) {
            return new Solid12(bounds[0], bounds[1]);
        }

        return new SolidN(bounds);
    }
}
