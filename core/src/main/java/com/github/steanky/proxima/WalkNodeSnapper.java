package com.github.steanky.proxima;

import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WalkNodeSnapper implements DirectionalNodeSnapper {
    private final int height;

    private final Vec3I[] hDeltas;
    private final Vec3I[] vDeltas;

    private final int fallTolerance;
    private final int jumpHeight;
    private final Space space;

    public WalkNodeSnapper(int width, int height, int fallTolerance, int jumpHeight, @NotNull Space space) {
        if ((width & 1) == 0) {
            throw new IllegalArgumentException("Width must be an odd number");
        }

        this.height = height;
        int halfWidth = width >> 1;

        this.hDeltas = new Vec3I[width * height];
        this.vDeltas = new Vec3I[width * width];

        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                //this is effectively a two-dimensional vector, z is always 0
                //how this is interpreted depends on the direction
                //these vectors are small enough that, unless we have really large entities, they will be cached
                hDeltas[h * width + w] = Vec3I.immutable(w - halfWidth, h, 0);
            }
        }

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                //vertical deltas are simple
                vDeltas[x * width + z] = Vec3I.immutable(x - halfWidth, 0, z - halfWidth);
            }
        }

        this.fallTolerance = fallTolerance;
        this.jumpHeight = jumpHeight;
        this.space = Objects.requireNonNull(space);
    }

    @Override
    public void snap(@NotNull Direction direction, @NotNull Node node, @NotNull NodeHandler handler) {
        int nX = node.x + direction.x();
        int nZ = node.z + direction.z();

        if (direction.y() == 0) {
            boolean tryJump = false;
            int highestY = -1; //only set if tryJump is true

            //first check: blocks we can run into horizontally
            for (Vec3I delta : hDeltas) {
                int x = nX + (direction.x() == 0 ? delta.x() : 0);
                int y = node.y + delta.y();
                int z = nZ + (direction.z() == 0 ? delta.x() : 0);

                Solid solid = space.solidAt(x, y, z);
                if (!solid.isEmpty()) {
                    Bounds3D bounds = solid.bounds();
                    if (delta.y() + bounds.lengthY() > jumpHeight) {
                        //fast exit: we can't go this way, there's a block we can't jump over
                        return;
                    }

                    highestY = y + 1; //hDeltas increases in height
                    tryJump = true;
                }
            }

            if (tryJump) {
                //first, check blocks above us
                //also limit jump height according to blocks we might run into
                int highestPossibleJump = jumpHeight;

                outer:
                for (int i = 0; i < jumpHeight; i++) {
                    int y = node.y + height + i;

                    for (Vec3I delta : vDeltas) {
                        int x = node.x + delta.x();
                        int z = node.z + delta.z();

                        Solid solid = space.solidAt(x, y, z);
                        if (!solid.isEmpty()) {
                            if (i == 0) {
                                //fast exit: we hit our head on a block directly above us
                                return;
                            }

                            if (highestY > y - height) {
                                //fast exit: the highest block we initially collided with is above where we just hit
                                return;
                            }

                            highestPossibleJump = i;
                            break outer;
                        }
                    }
                }

                //then, check blocks below
                int lastSolid = highestY;
                for (int i = highestY - node.y; i <= highestPossibleJump; i += height) {
                    boolean foundSolid = false;
                    for (Vec3I delta : hDeltas) {
                        int x = nX + (direction.x() == 0 ? delta.x() : 0);
                        int y = node.y + i + delta.y();
                        int z = nZ + (direction.z() == 0 ? delta.x() : 0);

                        Solid solid = space.solidAt(x, y, z);
                        if (!solid.isEmpty()) {
                            foundSolid = true;

                            if (y - lastSolid >= height) {
                                //found a gap we can fit in
                                handler.handle(node, nX, lastSolid, nZ);
                                return;
                            }

                            lastSolid = y + 1;
                            break;
                        }
                        else if ((y + 1) - lastSolid >= height) {
                            handler.handle(node, nX, lastSolid, nZ);
                            return;
                        }
                    }

                    if (!foundSolid) {
                        handler.handle(node, nX, lastSolid, nZ);
                        return;
                    }
                }

                return;
            }
        }
        else if(direction.y() == 1) {
            if (jumpHeight == 0) {
                return;
            }

            for (Vec3I delta : vDeltas) {
                int x = node.x + delta.x();
                int y = node.y + height;
                int z = node.z + delta.z();

                Solid solid = space.solidAt(x, y, z);
                if (!solid.isEmpty()) {
                    return;
                }
            }

            handler.handle(node, node.x, node.y + 1, node.z);
            return;
        }

        for (int i = 1; i <= fallTolerance; i++) { //check below us a maximum of fallTolerance blocks
            for (Vec3I delta : vDeltas) {
                int x = nX + delta.x();
                int y = node.y - i;
                int z = nZ + delta.z();

                Solid solid = space.solidAt(x, y, z);
                if (!solid.isEmpty()) { //we hit a solid on the way down
                    handler.handle(node, nX, y + 1, nZ);
                    return;
                }
            }
        }
    }
}
