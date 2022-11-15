package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.solid.SingletonSolid;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.toolkit.function.Wrapper;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.Vec3I;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalkNodeSnapperTest {
    private static double EPSILON = 1E-6;

    private record SolidPos(Solid solid, Vec3I pos) { }

    private static SolidPos solid(Solid solid, int x, int y, int z) {
        return new SolidPos(solid, Vec3I.immutable(x, y, z));
    }

    private static SolidPos full(int x, int y, int z) {
        return solid(Solid.FULL, x, y, z);
    }

    private static Node node(int x, int y, int z) {
        return node(x, y, z, 0);
    }

    private static Node node(int x, int y, int z, double yOffset) {
        return new Node(x, y, z, 0, 0, null, yOffset);
    }

    private static WalkNodeSnapper make(double width, double height, double fallTolerance, double jumpHeight,
            double epsilon, SolidPos... solids) {
        Bounds3I bounds = Bounds3I.immutable(-10, -10, -10, 20, 20, 20);
        HashSpace space = new HashSpace(bounds.originX(), bounds.originY(), bounds.originZ(), bounds.lengthX(),
                bounds.lengthY(), bounds.lengthZ());

        for (SolidPos solid : solids) {
            space.put(solid.pos, solid.solid);
        }

        return new WalkNodeSnapper(width, height, fallTolerance, jumpHeight, space, bounds, epsilon);
    }

    private static void assertSnap(WalkNodeSnapper snapper, Direction direction, Node node, NodeHandler handler) {
        Wrapper<Boolean> called = Wrapper.of(false);
        snapper.snap(direction, node, (node1, x, y, z, yOffset) -> {
            called.set(true);
            handler.handle(node1, x, y, z, yOffset);
        });

        assertTrue(called.get(), "handler was not called");
    }

    private static void walk(Direction direction, double width, double height, int x, int y, int z, double yo,
            int ex, int ey, int ez, double eOffset, SolidPos... pos) {
        WalkNodeSnapper snapper = make(width, height, 1, 1, EPSILON, pos);

        assertSnap(snapper, direction, node(x, y, z, yo), (node, x1, y1, z1, yOffset) -> {
            assertEquals(ex, x1, "x-coord");
            assertEquals(ey, y1, "y-coord");
            assertEquals(ez, z1, "z-coord");
            assertEquals(eOffset, yOffset, "yOffset");
        });
    }

    @Nested
    class FullWidth {
        @Nested
        class FullHeight {
            private static void walk(Direction direction, int x, int y, int z, double yo, int ex, int ey, int ez,
                    double eOffset, SolidPos... pos) {
                WalkNodeSnapperTest.walk(direction, 1, 1, x, y, z, yo, ex, ey, ez, eOffset, pos);
            }

            @Nested
            class FullBlocks {
                private static SolidPos[] flatWalkBlocks(int x, int y, int z) {
                    return new SolidPos[] {
                            full(x, y, z),
                            full(x + 1, y, z),
                            full(x - 1, y, z),
                            full(x, y, z + 1),
                            full(x, y, z - 1)
                    };
                }

                private static SolidPos[] jumpWalkBlocks(int x, int y, int z) {
                    return new SolidPos[] {
                            full(x, y, z),
                            full(x + 1, y + 1, z),
                            full(x - 1, y + 1, z),
                            full(x, y + 1, z + 1),
                            full(x, y + 1, z - 1)
                    };
                }

                @Test
                void straightWalkNorth() {
                    walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkEast() {
                    walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkSouth() {
                    walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkWest() {
                    walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkNorthOffset() {
                    walk(Direction.NORTH, 0, 1, 0, 0.5, 0, 1, -1, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkEastOffset() {
                    walk(Direction.EAST, 0, 1, 0, 0.5,1, 1, 0, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkSouthOffset() {
                    walk(Direction.SOUTH, 0, 1, 0, 0.5, 0, 1, 1, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkWestOffset() {
                    walk(Direction.WEST, 0, 1, 0, 0.5, -1, 1, 0, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void jumpWalkNorth() {
                    walk(Direction.NORTH, 0, 1, 0, 0.5, 0, 2, -1, 0,
                            jumpWalkBlocks(0, 0, 0));
                }

                @Test
                void jumpWalkEast() {
                    walk(Direction.EAST, 0, 1, 0, 0.5, 1, 2, 0, 0,
                            jumpWalkBlocks(0, 0, 0));
                }

                @Test
                void jumpWalkSouth() {
                    walk(Direction.SOUTH, 0, 1, 0, 0.5, 0, 2, 1, 0,
                            jumpWalkBlocks(0, 0, 0));
                }

                @Test
                void jumpWalkWest() {
                    walk(Direction.WEST, 0, 1, 0, 0.5, -1, 2, 0, 0,
                            jumpWalkBlocks(0, 0, 0));
                }
            }

            @Nested
            class HalfBlocks {
                private static final Solid LOWER_HALF_BLOCK = new SingletonSolid(Bounds3D.immutable(0, 0, 0,
                        1, 0.5, 1));

                private static SolidPos[] flatWalkBlocks(int x, int y, int z) {
                    return new SolidPos[] {
                            full(x, y, z),
                            solid(LOWER_HALF_BLOCK, x + 1, y, z),
                            solid(LOWER_HALF_BLOCK,x - 1, y, z),
                            solid(LOWER_HALF_BLOCK, x, y, z + 1),
                            solid(LOWER_HALF_BLOCK, x, y, z - 1)
                    };
                }

                private static SolidPos[] jumpWalkBlocks(int x, int y, int z) {
                    return new SolidPos[] {
                            full(x, y, z),
                            solid(LOWER_HALF_BLOCK,x + 1, y + 1, z),
                            solid(LOWER_HALF_BLOCK,x - 1, y + 1, z),
                            solid(LOWER_HALF_BLOCK, x, y + 1, z + 1),
                            solid(LOWER_HALF_BLOCK, x, y + 1, z - 1)
                    };
                }

                @Test
                void straightWalkNorth() {
                    walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkEast() {
                    walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkSouth() {
                    walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void straightWalkWest() {
                    walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0,
                            flatWalkBlocks(0, 0, 0));
                }

                @Test
                void jumpWalkNorth() {
                    walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0.5,
                            jumpWalkBlocks(0, 0, 0));
                }

                @Test
                void jumpWalkEast() {
                    walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0.5,
                            jumpWalkBlocks(0, 0, 0));
                }

                @Test
                void jumpWalkSouth() {
                    walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0.5,
                            jumpWalkBlocks(0, 0, 0));
                }

                @Test
                void jumpWalkWest() {
                    walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0.5,
                            jumpWalkBlocks(0, 0, 0));
                }
            }
        }
    }

}



























