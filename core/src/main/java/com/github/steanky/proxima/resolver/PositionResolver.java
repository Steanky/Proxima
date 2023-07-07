package com.github.steanky.proxima.resolver;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface PositionResolver {
    PositionResolver FLOORED = Vec3I::immutableFloored;

    static @NotNull PositionResolver seekBelow(@NotNull Space space, int searchHeight, double width, double epsilon) {
        return new ClosestBelow(space, searchHeight, width, epsilon);
    }

    static @NotNull PositionResolver asIfByInitial(@NotNull NodeSnapper snapper, int searchHeight, double width, double epsilon) {
        Objects.requireNonNull(snapper, "snapper");

        PositionResolver seekBelow = seekBelow(snapper.space(), searchHeight, width, epsilon);

        return new PositionResolver() {
            private static final Direction[] DIRECTIONS =
                    new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

            @Override
            public @NotNull Vec3I resolve(double x, double y, double z) {
                Vec3I standingOn = seekBelow.resolve(x, y, z);

                int isx = standingOn.x();
                int isy = standingOn.y();
                int isz = standingOn.z();

                double closestVectorDistance = Double.POSITIVE_INFINITY;
                Vec3I closestVector = null;

                long result = snapper.checkInitial(x, y, z, isx, isy, isz);
                if (result != NodeSnapper.FAIL) {
                    return Vec3I.immutable(isx, NodeSnapper.blockHeight(result), isz);
                }

                for (Direction direction : DIRECTIONS) {
                    int inx = (int) Math.floor(x + direction.x);
                    int iny = (int) Math.floor(y + direction.y);
                    int inz = (int) Math.floor(z + direction.z);

                    result = snapper.checkInitial(x, y, z, inx, iny, inz);
                    if (result != NodeSnapper.FAIL) {
                        float height = NodeSnapper.height(result);

                        double thisDistance = Vec3D.distanceSquared(x, y, z, inx + 0.5, height, inz + 0.5);
                        if (thisDistance < closestVectorDistance) {
                            closestVectorDistance = thisDistance;
                            closestVector = Vec3I.immutable(inx, NodeSnapper.blockHeight(result), inz);
                        }
                    }
                }

                return closestVector == null ? standingOn : closestVector;
            }
        };
    }

    @NotNull Vec3I resolve(double x, double y, double z);

    default @NotNull Vec3I resolve(@NotNull Vec3D vec) {
        return resolve(vec.x(), vec.y(), vec.z());
    }
}
