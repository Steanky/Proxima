package com.github.steanky.proxima.resolver;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

class SnappingResolver implements PositionResolver {
    private final NodeSnapper snapper;
    private final Direction[] directions;

    SnappingResolver(@NotNull NodeSnapper snapper, @NotNull Direction @NotNull [] directions) {
        this.snapper = Objects.requireNonNull(snapper);
        this.directions = Arrays.copyOf(directions, directions.length);

        for (Direction direction : this.directions) {
            Objects.requireNonNull(direction, "direction array element");
        }
    }

    @Override
    public @NotNull Vec3I resolve(double x, double y, double z) {
        int bx = (int)Math.floor(x);
        int by = (int)Math.floor(y);
        int bz = (int)Math.floor(z);

        if (!Float.isNaN(snapper.checkInitial(x, y, z, bx, by, bz))) {
            return Vec3I.immutable(bx, by, bz);
        }

        for (Direction direction : directions) {
            int tx = bx + direction.x;
            int ty = by + direction.y;
            int tz = bz + direction.z;

            if (Vec3D.distanceSquared(x, y, z, tx + 0.5, ty, tz + 0.5) > 1) {
                continue;
            }

            if (!Float.isNaN(snapper.checkInitial(x, y, z, tx, ty, tz))) {
                return Vec3I.immutable(tx, ty, tz);
            }
        }

        return FLOORED.resolve(x, y, z);
    }
}
