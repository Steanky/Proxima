package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.HashSpace;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.Vec3I;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BasicNodeSnapperTest {
    private static final double EPSILON = 1E-6;

    private static final Solid LOWER_HALF_BLOCK = Solid.of(Bounds3D.immutable(0, 0, 0,
            1, 0.5, 1));

    private static final Solid UPPER_HALF_BLOCK = Solid.of(Bounds3D.immutable(0, 0.5, 0,
            1, 0.5, 1));

    private static final Solid PARTIAL_BLOCK_NORTH = Solid.of(Bounds3D.immutable(0, 0, 0,
            1, 1, 0.0625));

    private static final Solid PARTIAL_BLOCK_SOUTH = Solid.of(Bounds3D.immutable(0, 0,
            0.9375, 1, 1, 0.0625));

    private static final Solid PARTIAL_BLOCK_EAST = Solid.of(Bounds3D.immutable(0.9375, 0,
            0, 0.0625, 1, 1));

    private static final Solid PARTIAL_BLOCK_WEST = Solid.of(Bounds3D.immutable(0, 0,
            0, 0.0625, 1, 1));

    private record SolidPos(Solid solid, Vec3I pos) { }

    private static SolidPos solid(Solid solid, int x, int y, int z) {
        return new SolidPos(solid, Vec3I.immutable(x, y, z));
    }

    private static SolidPos full(int x, int y, int z) {
        return solid(Solid.FULL, x, y, z);
    }

    private static SolidPos stairs(int x, int y, int z) {
        return solid(Solid.of(Bounds3D.immutable(0, 0, 0, 1, 0.5, 1),
                Bounds3D.immutable(0, 0.5, 0.5, 1, 0.5, 0.5)), x, y, z);
    }

    private static Node node(int x, int y, int z) {
        return node(x, y, z, 0);
    }

    private static Node node(int x, int y, int z, float yOffset) {
        return new Node(x, y, z, 0, 0, null, yOffset);
    }

    private static BasicNodeSnapper make(double width, double height, double fallTolerance, double jumpHeight,
            double epsilon, SolidPos... solids) {
        Bounds3I bounds = Bounds3I.immutable(-10, -10, -10, 20, 20, 20);
        HashSpace space = new HashSpace(bounds.originX(), bounds.originY(), bounds.originZ(), bounds.lengthX(),
                bounds.lengthY(), bounds.lengthZ());

        for (SolidPos solid : solids) {
            space.put(solid.pos, solid.solid);
        }

        return new BasicNodeSnapper(width, height, fallTolerance, jumpHeight, space, true, epsilon);
    }

    private static void assertSnap(BasicNodeSnapper snapper, Direction direction, Node node, NodeHandler handler) {
        long val = snapper.snap(direction, node.x, node.y, node.z, node.yOffset);
        if (val != NodeSnapper.FAIL) {
            int y = NodeSnapper.height(val);
            float offset = NodeSnapper.offset(val);

            handler.handle(node, null, node.x + direction.x, y, node.z + direction.z, offset);
        }
        else {
            fail("snapper returned FAIL");
        }
    }

    private static String decodeToString(long snapResult) {
        return "height=" + NodeSnapper.height(snapResult) + ", offset=" + NodeSnapper.offset(snapResult);
    }

    private static void assertNoSnap(BasicNodeSnapper snapper, Direction direction, Node node) {
        long result;
        assertEquals(NodeSnapper.FAIL, result = snapper.snap(direction, node.x, node.y, node.z, node.yOffset),
                "node did not fail: " + decodeToString(result));
    }

    private static void walk(Direction direction, double width, double height, int x, int y, int z, float yo,
            int ex, int ey, int ez, double eOffset, SolidPos... pos) {
        BasicNodeSnapper snapper = make(width, height, 1, 1, EPSILON, pos);

        assertSnap(snapper, direction, node(x, y, z, yo), (node, other, x1, y1, z1, yOffset) -> {
            assertEquals(ex, x1, "x-coord");
            assertEquals(ey, y1, "y-coord");
            assertEquals(ez, z1, "z-coord");
            assertEquals(eOffset, yOffset, "yOffset");
        });
    }

    private static void noWalk(Direction direction, double width, double height, int x, int y, int z, float yo,
            SolidPos... pos) {
        BasicNodeSnapper snapper = make(width, height, 1, 1, EPSILON, pos);
        assertNoSnap(snapper, direction, node(x, y, z, yo));
    }

    @Nested
    class Walk {
        @Nested
        class FullWidth {
            @Nested
            class FullHeight {
                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez,
                        double eOffset, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 1, 1, x, y, z, yo, ex, ey, ez, eOffset, pos);
                }

                @Nested
                class Stairs {
                    private static SolidPos[] stairsBelow(int x, int y, int z) {
                        return new SolidPos[] {
                                stairs(x, y, z),
                                stairs(x + 1, y, z),
                                stairs(x - 1, y, z),
                                stairs(x, y, z + 1),
                                stairs(x, y, z - 1),
                        };
                    }

                    private static SolidPos[] stairsAround(int x, int y, int z) {
                        return new SolidPos[] {
                                stairs(x, y, z),

                                stairs(x + 1, y, z),
                                stairs(x - 1, y, z),
                                stairs(x, y, z + 1),
                                stairs(x, y, z - 1),

                                stairs(x + 1, y + 1, z),
                                stairs(x - 1, y + 1, z),
                                stairs(x, y + 1, z + 1),
                                stairs(x, y + 1, z - 1),
                        };
                    }

                    @Test
                    void straightWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0,
                                stairsBelow(0, 0, 0));
                    }

                    @Test
                    void straightWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0,
                                stairsBelow(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0,
                                stairsBelow(0, 0, 0));
                    }

                    @Test
                    void straightWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0,
                                stairsBelow(0, 0, 0));
                    }

                    @Test
                    void straightJumpNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 2, -1, 0,
                                stairsAround(0, 0, 0));
                    }

                    @Test
                    void straightJumpEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 2, 0, 0,
                                stairsAround(0, 0, 0));
                    }

                    @Test
                    void straightJumpSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 2, 1, 0,
                                stairsAround(0, 0, 0));
                    }

                    @Test
                    void straightJumpWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 2, 0, 0,
                                stairsAround(0, 0, 0));
                    }
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

                    private static SolidPos[] fallWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {
                                full(x, y, z),
                                full(x + 1, y - 1, z),
                                full(x - 1, y - 1, z),
                                full(x, y - 1, z + 1),
                                full(x, y - 1, z - 1)
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

                    private static SolidPos[] enclosedBlocks(int x, int y, int z) {
                        return new SolidPos[] {
                                full(x + 1, y, z),
                                full(x - 1, y, z),
                                full(x, y, z + 1),
                                full(x, y, z - 1),
                                full(x, y + 1, z),
                                full(x, y - 1, z),
                        };
                    }

                    @Test
                    void fallSingleWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 0, -1, 0,
                                fallWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void fallSingleWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 0, 0, 0,
                                fallWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void fallSingleWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 0, 1, 0,
                                fallWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void fallSingleWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 0, 0, 0,
                                fallWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void enclosedWalkNorth() {
                        noWalk(Direction.NORTH, 1, 1, 0, 0, 0, 0.0F,
                                enclosedBlocks(0, 0, 0));
                    }

                    @Test
                    void enclosedWalkEast() {
                        noWalk(Direction.EAST, 1, 1, 0, 0, 0, 0.0F,
                                enclosedBlocks(0, 0, 0));
                    }

                    @Test
                    void enclosedWalkSouth() {
                        noWalk(Direction.SOUTH, 1, 1, 0, 0, 0, 0.0F,
                                enclosedBlocks(0, 0, 0));
                    }

                    @Test
                    void enclosedWalkWest() {
                        noWalk(Direction.WEST, 1, 1, 0, 0, 0, 0.0F,
                                enclosedBlocks(0, 0, 0));
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
                        walk(Direction.NORTH, 0, 1, 0, 0.5F, 0, 1, -1, 0,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEastOffset() {
                        walk(Direction.EAST, 0, 1, 0, 0.5F,1, 1, 0, 0,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouthOffset() {
                        walk(Direction.SOUTH, 0, 1, 0, 0.5F, 0, 1, 1, 0,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWestOffset() {
                        walk(Direction.WEST, 0, 1, 0, 0.5F, -1, 1, 0, 0,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0.5F, 0, 2, -1, 0,
                                jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0.5F, 1, 2, 0, 0,
                                jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0.5F, 0, 2, 1, 0,
                                jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0.5F, -1, 2, 0, 0,
                                jumpWalkBlocks(0, 0, 0));
                    }
                }

                @Nested
                class HalfBlocks {
                    private static final Solid LOWER_HALF_BLOCK = Solid.of(Bounds3D.immutable(0, 0, 0,
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
                        walk(Direction.NORTH, 0, 0, 0, 0.5F, 0, 0, -1, 0.5,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEast() {
                        walk(Direction.EAST, 0, 0, 0, 0.5F, 1, 0, 0, 0.5,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouth() {
                        walk(Direction.SOUTH, 0, 0, 0, 0.5F, 0, 0, 1, 0.5,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWest() {
                        walk(Direction.WEST, 0, 0, 0, 0.5F, -1, 0, 0, 0.5,
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

        @Nested
        class PartialWidth {
            @Nested
            class PartialHeight {
                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez,
                        double eOffset, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 0.6, 1.95, x, y, z, yo, ex, ey, ez, eOffset, pos);
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
                        walk(Direction.NORTH, 0, 1, 0, 0.5F, 0, 1, -1, 0,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEastOffset() {
                        walk(Direction.EAST, 0, 1, 0, 0.5F,1, 1, 0, 0,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouthOffset() {
                        walk(Direction.SOUTH, 0, 1, 0, 0.5F, 0, 1, 1, 0,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWestOffset() {
                        walk(Direction.WEST, 0, 1, 0, 0.5F, -1, 1, 0, 0,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0.5F, 0, 2, -1, 0,
                                jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0.5F, 1, 2, 0, 0,
                                jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0.5F, 0, 2, 1, 0,
                                jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0.5F, -1, 2, 0, 0,
                                jumpWalkBlocks(0, 0, 0));
                    }
                }

                @Nested
                class HalfBlocks {
                    private static SolidPos[] flatWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {
                                full(x, y, z),
                                solid(LOWER_HALF_BLOCK, x + 1, y, z),
                                solid(LOWER_HALF_BLOCK,x - 1, y, z),
                                solid(LOWER_HALF_BLOCK, x, y, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y, z - 1)
                        };
                    }

                    private static SolidPos[] intersectPartial(int x, int y, int z, Solid solid) {
                        return new SolidPos[] {
                                full(x, y, z),
                                solid(solid, x, y + 1, z),
                                full(x + 1, y, z),
                                full(x - 1, y, z),
                                full(x , y, z + 1),
                                full(x, y, z -1)
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

                    private static SolidPos[] stepUpFromSlabBlocks(int x, int y, int z) {
                        return new SolidPos[] {
                                solid(LOWER_HALF_BLOCK, x, y, z),
                                full(x + 1, y, z),
                                full(x - 1, y, z),
                                full(x, y, z + 1),
                                full(x, y, z - 1)
                        };
                    }

                    private static SolidPos[] jumpUpFromSlabBlocks(int x, int y, int z) {
                        return new SolidPos[] {
                                solid(LOWER_HALF_BLOCK, x, y, z),
                                full(x + 1, y, z),
                                full(x - 1, y, z),
                                full(x, y, z + 1),
                                full(x, y, z - 1),
                                solid(LOWER_HALF_BLOCK, x + 1, y + 1, z),
                                solid(LOWER_HALF_BLOCK, x - 1, y + 1, z),
                                solid(LOWER_HALF_BLOCK, x, y + 1, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y + 1, z - 1)
                        };
                    }

                    private static SolidPos[] jumpUpFromSlabBlocked(int x, int y, int z) {
                        return new SolidPos[] {
                                solid(LOWER_HALF_BLOCK, x, y, z),
                                full(x + 1, y, z),
                                full(x - 1, y, z),
                                full(x, y, z + 1),
                                full(x, y, z - 1),
                                solid(LOWER_HALF_BLOCK, x + 1, y + 1, z),
                                solid(LOWER_HALF_BLOCK, x - 1, y + 1, z),
                                solid(LOWER_HALF_BLOCK, x, y + 1, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y + 1, z - 1),
                                solid(UPPER_HALF_BLOCK, x, y + 2, z)
                        };
                    }

                    @Test
                    void jumpUpFromSlabBlockedNorth() {
                        noWalk(Direction.NORTH, 0.6, 1.95, 0, 0, 0, 0.5F,
                                jumpUpFromSlabBlocked(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabBlockedEast() {
                        noWalk(Direction.EAST, 0.6, 1.95, 0, 0, 0, 0.5F,
                                jumpUpFromSlabBlocked(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabBlockedSouth() {
                        noWalk(Direction.SOUTH, 0.6, 1.95, 0, 0, 0, 0.5F,
                                jumpUpFromSlabBlocked(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabBlockedWest() {
                        noWalk(Direction.WEST, 0.6, 1.95, 0, 0, 0, 0.5F,
                                jumpUpFromSlabBlocked(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabNorth() {
                        walk(Direction.NORTH, 0, 0, 0, 0.5F, 0, 1, -1, 0.5,
                                jumpUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabEast() {
                        walk(Direction.EAST, 0, 0, 0, 0.5F, 1, 1, 0, 0.5,
                                jumpUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabSouth() {
                        walk(Direction.SOUTH, 0, 0, 0, 0.5F, 0, 1, 1, 0.5,
                                jumpUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabWest() {
                        walk(Direction.WEST, 0, 0, 0, 0.5F, -1, 1, 0, 0.5,
                                jumpUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void stepUpFromSlabNorth() {
                        walk(Direction.NORTH, 0, 0, 0, 0.5F, 0, 1, -1, 0,
                                stepUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void stepUpFromSlabEast() {
                        walk(Direction.EAST, 0, 0, 0, 0.5F, 1, 1, 0, 0,
                                stepUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void stepUpFromSlabSouth() {
                        walk(Direction.SOUTH, 0, 0, 0, 0.5F, 0, 1, 1, 0,
                                stepUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void stepUpFromSlabWest() {
                        walk(Direction.WEST, 0, 0, 0, 0.5F, -1, 1, 0, 0,
                                stepUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void walkNorthMissSouthPartialBlock() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void walkEastMissSouthPartialBlock() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void walkWestMissSouthPartialBlock() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void walkSouthMissNorthPartialBlock() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void walkEastMissNorthPartialBlock() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void walkWestMissNorthPartialBlock() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void walkSouthMissEastPartialBlock() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void walkNorthMissEastPartialBlock() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void walkWestMissEastPartialBlock() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void walkNorthMissWestPartialBlock() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void walkEastMissWestPartialBlock() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void walkSouthMissWestPartialBlock() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void walkNorthIntersectPartialBlock() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 2, -1, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void walkEastIntersectPartialBlock() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 2, 0, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void walkSouthIntersectPartialBlock() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 2, 1, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void walkWestIntersectPartialBlock() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 2, 0, 0,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void straightWalkNorth() {
                        walk(Direction.NORTH, 0, 0, 0, 0.5F, 0, 0, -1, 0.5,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEast() {
                        walk(Direction.EAST, 0, 0, 0, 0.5F, 1, 0, 0, 0.5,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouth() {
                        walk(Direction.SOUTH, 0, 0, 0, 0.5F, 0, 0, 1, 0.5,
                                flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWest() {
                        walk(Direction.WEST, 0, 0, 0, 0.5F, -1, 0, 0, 0.5,
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

        @Nested
        class LargePartialWidth {
            @Nested
            class FullHeight {
                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez,
                        double eOffset, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 2, 2, x, y, z, yo, ex, ey, ez, eOffset, pos);
                }

                private static void noWalk(Direction direction, int x, int y, int z, float yo, SolidPos... pos) {
                    BasicNodeSnapperTest.noWalk(direction, 2, 2, x, y, z, yo, pos);
                }

                private static SolidPos[] intersectPartial(int x, int y, int z, Solid solid, Direction direction) {
                    return new SolidPos[] {
                            full(x, y, z),
                            full(x + 1, y, z),
                            full(x - 1, y, z),
                            full(x, y, z + 1),
                            full(x, y, z - 1),
                            solid(solid, x + direction.x, y + 1, z + direction.z)
                    };
                }

                private static SolidPos[] blockedBySlabsPartial(int x, int y, int z, Direction direction) {
                    return new SolidPos[] {
                            full(x, y, z),
                            full(x + direction.x, y, z + direction.z),
                            full(x + 2 * direction.x, y, z + 2 * direction.z),
                            solid(LOWER_HALF_BLOCK, x + 2 * direction.x, y + 1, z + 2 * direction.z),
                            solid(UPPER_HALF_BLOCK, x + 2 * direction.x, y + 2, z + 2 * direction.z)
                    };
                }

                @Test
                void walkNorthBlockedBySlabsPartial() {
                    noWalk(Direction.NORTH, 0, 1, 0, 0, blockedBySlabsPartial(0, 0, 0,
                            Direction.NORTH));
                }

                @Test
                void walkEastBlockedBySlabsPartial() {
                    noWalk(Direction.EAST, 0, 1, 0, 0, blockedBySlabsPartial(0, 0, 0,
                            Direction.EAST));
                }

                @Test
                void walkSouthBlockedBySlabsPartial() {
                    noWalk(Direction.SOUTH, 0, 1, 0, 0, blockedBySlabsPartial(0, 0, 0,
                            Direction.SOUTH));
                }

                @Test
                void walkWestBlockedBySlabsPartial() {
                    noWalk(Direction.WEST, 0, 1, 0, 0, blockedBySlabsPartial(0, 0, 0,
                            Direction.WEST));
                }

                @Test
                void walkNorthIntersectPartial() {
                    walk(Direction.NORTH, 0, 1, 0, 0, 0, 2, -1, 0,
                            intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH, Direction.NORTH));
                }

                @Test
                void walkEastIntersectPartial() {
                    walk(Direction.EAST, 0, 1, 0, 0, 1, 2, 0, 0,
                            intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST, Direction.EAST));
                }

                @Test
                void walkSouthIntersectPartial() {
                    walk(Direction.SOUTH, 0, 1, 0, 0, 0, 2, 1, 0,
                            intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH, Direction.SOUTH));
                }

                @Test
                void walkWestIntersectPartial() {
                    walk(Direction.WEST, 0, 1, 0, 0, -1, 2, 0, 0,
                            intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST, Direction.WEST));
                }
            }
        }
    }

    @Nested
    class CheckInitial {
        private SolidPos[] noCollisionBelow(int x, int y, int z) {
            return new SolidPos[] {
                    full(x, y, z),
                    full(x + 1, y, z),
                    full(x - 1, y, z),
                    full(x, y, z + 1),
                    full(x, y, z - 1)
            };
        }

        @Test
        void smallBoundsNoCollision() {
            BasicNodeSnapper snapper = make(0.3, 2, 0, 0, EPSILON,
                    noCollisionBelow(0, 0, 0));

            float res = snapper.checkInitial(0.6, 1, 0.6, 0.5, 1, 0.5);
            assertEquals(0, res);
        }
    }
}