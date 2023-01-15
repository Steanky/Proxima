package com.github.steanky.proxima.snapper;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.NodeHandler;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.HashSpace;
import com.github.steanky.vector.Bounds3D;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.Vec3D;
import com.github.steanky.vector.Vec3I;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleConsumer;

import static org.junit.jupiter.api.Assertions.*;

class BasicNodeSnapperTest {
    public static final double EPSILON = 1E-6;

    public static final Solid LOWER_HALF_BLOCK = Solid.of(Bounds3D.immutable(0, 0, 0, 1, 0.5, 1));

    public static final Solid UPPER_HALF_BLOCK = Solid.of(Bounds3D.immutable(0, 0.5, 0, 1, 0.5, 1));

    public static final Solid PARTIAL_BLOCK_NORTH = Solid.of(Bounds3D.immutable(0, 0, 0, 1, 1, 0.0625));

    public static final Solid PARTIAL_BLOCK_SOUTH = Solid.of(Bounds3D.immutable(0, 0, 0.9375, 1, 1, 0.0625));

    public static final Solid PARTIAL_BLOCK_EAST = Solid.of(Bounds3D.immutable(0.9375, 0, 0, 0.0625, 1, 1));

    public static final Solid PARTIAL_BLOCK_WEST = Solid.of(Bounds3D.immutable(0, 0, 0, 0.0625, 1, 1));

    public static final Solid SMALL_CENTRAL_SOLID = Solid.of(Bounds3D.immutable(0.45, 0, 0.45, 0.1, 1, 0.1));

    public static final Solid SMALL_UPPER_LEFT_SOLID = Solid.of(Bounds3D.immutable(0, 0, 0.7, 0.1, 1, 0.1));

    public static final Solid NEARLY_FULL_SOLID = Solid.of(Bounds3D.immutable(0.1, 0, 0.1, 0.8, 1, 0.8));

    public static final Solid STAIRS =
            Solid.of(Bounds3D.immutable(0, 0, 0, 1, 0.5, 1), Bounds3D.immutable(0, 0.5, 0.5, 1, 0.5, 0.5));

    private static SolidPos solid(Solid solid, int x, int y, int z) {
        return new SolidPos(solid, Vec3I.immutable(x, y, z));
    }

    private static SolidPos full(int x, int y, int z) {
        return solid(Solid.FULL, x, y, z);
    }

    private static SolidPos stairs(int x, int y, int z) {
        return solid(STAIRS, x, y, z);
    }

    private static Node node(int x, int y, int z) {
        return node(x, y, z, 0);
    }

    private static Node node(int x, int y, int z, float yOffset) {
        return new Node(x, y, z, 0, 0, null, yOffset);
    }

    private static BasicNodeSnapper make(double width, double height, double fallTolerance, double jumpHeight, double epsilon, SolidPos... solids) {
        Bounds3I bounds = Bounds3I.immutable(-100, -100, -100, 200, 200, 200);
        HashSpace space =
                new HashSpace(bounds.originX(), bounds.originY(), bounds.originZ(), bounds.lengthX(), bounds.lengthY(),
                        bounds.lengthZ());

        for (SolidPos solid : solids) {
            space.put(solid.pos, solid.solid);
        }

        return new BasicNodeSnapper(space, width, height, fallTolerance, jumpHeight, epsilon);
    }

    private static void assertSnap(BasicNodeSnapper snapper, Direction direction, Node node, NodeHandler handler, boolean isIntermediateJump) {
        long val = snapper.snap(direction, node.x, node.y, node.z, node.blockOffset);
        if (val != NodeSnapper.FAIL) {
            int blockHeight = NodeSnapper.blockHeight(val);
            float blockOffset = NodeSnapper.blockOffset(val);
            float jumpOffset = NodeSnapper.jumpOffset(val);
            boolean intermediateJump = NodeSnapper.intermediateJump(val);

            if (isIntermediateJump) {
                assertTrue(intermediateJump, "expected an intermediate jump");
            } else {
                assertFalse(intermediateJump, "expected a non-intermediate jump");
            }

            handler.handle(node, null, node.x + direction.x, blockHeight, node.z + direction.z, blockOffset,
                    jumpOffset);
        } else {
            fail("snapper returned FAIL");
        }
    }

    private static String decodeToString(long snapResult) {
        return "height=" + NodeSnapper.height(snapResult) + ", offset=" + NodeSnapper.jumpOffset(snapResult);
    }

    private static void assertNoSnap(BasicNodeSnapper snapper, Direction direction, Node node) {
        long result;
        assertEquals(NodeSnapper.FAIL, result = snapper.snap(direction, node.x, node.y, node.z, node.blockOffset),
                "node did not fail: " + decodeToString(result));
    }

    private static void walk(Direction direction, double width, double height, int x, int y, int z, float yo, int ex, int ey, int ez, double eOffset, boolean intermediate, SolidPos... pos) {
        BasicNodeSnapper snapper = make(width, height, 1, 1, EPSILON, pos);

        assertSnap(snapper, direction, node(x, y, z, yo), (node, other, x1, y1, z1, blockOffset, jumpOffset) -> {
            assertEquals(ex, x1, "x-coord");
            assertEquals(ey, y1, "y-coord");
            assertEquals(ez, z1, "z-coord");
            assertEquals(eOffset, blockOffset, "blockOffset");
        }, intermediate);
    }

    private static void noWalk(Direction direction, double width, double height, int x, int y, int z, float yo, SolidPos... pos) {
        BasicNodeSnapper snapper = make(width, height, 1, 1, EPSILON, pos);
        assertNoSnap(snapper, direction, node(x, y, z, yo));
    }

    private record SolidPos(Solid solid, Vec3I pos) {
    }

    @Nested
    class Walk {
        @Nested
        class FullWidth {
            @Nested
            class FullHeight {
                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez, double eOffset, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 1, 1, x, y, z, yo, ex, ey, ez, eOffset, false, pos);
                }

                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez, double eOffset, boolean intermediate, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 1, 1, x, y, z, yo, ex, ey, ez, eOffset, intermediate, pos);
                }

                @Nested
                class Stairs {
                    private static SolidPos[] stairsBelow(int x, int y, int z) {
                        return new SolidPos[] {stairs(x, y, z), stairs(x + 1, y, z), stairs(x - 1, y, z),
                                stairs(x, y, z + 1), stairs(x, y, z - 1),};
                    }

                    private static SolidPos[] stairsAround(int x, int y, int z) {
                        return new SolidPos[] {stairs(x, y, z),

                                stairs(x + 1, y, z), stairs(x - 1, y, z), stairs(x, y, z + 1), stairs(x, y, z - 1),

                                stairs(x + 1, y + 1, z), stairs(x - 1, y + 1, z), stairs(x, y + 1, z + 1),
                                stairs(x, y + 1, z - 1),};
                    }

                    @Test
                    void straightWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0, stairsBelow(0, 0, 0));
                    }

                    @Test
                    void straightWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0, stairsBelow(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0, stairsBelow(0, 0, 0));
                    }

                    @Test
                    void straightWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0, stairsBelow(0, 0, 0));
                    }

                    @Test
                    void straightJumpNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 2, -1, 0, stairsAround(0, 0, 0));
                    }

                    @Test
                    void straightJumpEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 2, 0, 0, stairsAround(0, 0, 0));
                    }

                    @Test
                    void straightJumpSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 2, 1, 0, stairsAround(0, 0, 0));
                    }

                    @Test
                    void straightJumpWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 2, 0, 0, stairsAround(0, 0, 0));
                    }
                }

                @Nested
                class FullBlocks {
                    private static SolidPos[] flatWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                                full(x, y, z - 1)};
                    }

                    private static SolidPos[] fallWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), full(x + 1, y - 1, z), full(x - 1, y - 1, z),
                                full(x, y - 1, z + 1), full(x, y - 1, z - 1)};
                    }

                    private static SolidPos[] jumpWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), full(x + 1, y + 1, z), full(x - 1, y + 1, z),
                                full(x, y + 1, z + 1), full(x, y + 1, z - 1)};
                    }

                    private static SolidPos[] enclosedBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                                full(x, y, z - 1), full(x, y + 1, z), full(x, y - 1, z),};
                    }

                    private static SolidPos[] intermediateJumpThenFall(int x, int y, int z, Solid solid) {
                        return new SolidPos[] {full(x, y, z), solid(solid, x, y + 1, z)};
                    }

                    @Test
                    void jumpNorthThenFall() {
                        noWalk(Direction.NORTH, 0.5, 1, 0, 1, 0, 0,
                                intermediateJumpThenFall(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void jumpSouthThenFall() {
                        noWalk(Direction.SOUTH, 0.5, 1, 0, 1, 0, 0,
                                intermediateJumpThenFall(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void jumpEastThenFall() {
                        noWalk(Direction.EAST, 0.5, 1, 0, 1, 0, 0,
                                intermediateJumpThenFall(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void jumpWestThenFall() {
                        noWalk(Direction.WEST, 0.5, 1, 0, 1, 0, 0,
                                intermediateJumpThenFall(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void fallSingleWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 0, -1, 0, fallWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void fallSingleWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 0, 0, 0, fallWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void fallSingleWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 0, 1, 0, fallWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void fallSingleWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 0, 0, 0, fallWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void enclosedWalkNorth() {
                        noWalk(Direction.NORTH, 1, 1, 0, 0, 0, 0.0F, enclosedBlocks(0, 0, 0));
                    }

                    @Test
                    void enclosedWalkEast() {
                        noWalk(Direction.EAST, 1, 1, 0, 0, 0, 0.0F, enclosedBlocks(0, 0, 0));
                    }

                    @Test
                    void enclosedWalkSouth() {
                        noWalk(Direction.SOUTH, 1, 1, 0, 0, 0, 0.0F, enclosedBlocks(0, 0, 0));
                    }

                    @Test
                    void enclosedWalkWest() {
                        noWalk(Direction.WEST, 1, 1, 0, 0, 0, 0.0F, enclosedBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkNorthOffset() {
                        walk(Direction.NORTH, 0, 1, 0, 0.5F, 0, 1, -1, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEastOffset() {
                        walk(Direction.EAST, 0, 1, 0, 0.5F, 1, 1, 0, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouthOffset() {
                        walk(Direction.SOUTH, 0, 1, 0, 0.5F, 0, 1, 1, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWestOffset() {
                        walk(Direction.WEST, 0, 1, 0, 0.5F, -1, 1, 0, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0.5F, 0, 2, -1, 0, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0.5F, 1, 2, 0, 0, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0.5F, 0, 2, 1, 0, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0.5F, -1, 2, 0, 0, jumpWalkBlocks(0, 0, 0));
                    }
                }

                @Nested
                class HalfBlocks {
                    private static final Solid LOWER_HALF_BLOCK = Solid.of(Bounds3D.immutable(0, 0, 0, 1, 0.5, 1));

                    private static SolidPos[] flatWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), solid(LOWER_HALF_BLOCK, x + 1, y, z),
                                solid(LOWER_HALF_BLOCK, x - 1, y, z), solid(LOWER_HALF_BLOCK, x, y, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y, z - 1)};
                    }

                    private static SolidPos[] jumpWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), solid(LOWER_HALF_BLOCK, x + 1, y + 1, z),
                                solid(LOWER_HALF_BLOCK, x - 1, y + 1, z), solid(LOWER_HALF_BLOCK, x, y + 1, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y + 1, z - 1)};
                    }

                    @Test
                    void straightWalkNorth() {
                        walk(Direction.NORTH, 0, 0, 0, 0.5F, 0, 0, -1, 0.5, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEast() {
                        walk(Direction.EAST, 0, 0, 0, 0.5F, 1, 0, 0, 0.5, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouth() {
                        walk(Direction.SOUTH, 0, 0, 0, 0.5F, 0, 0, 1, 0.5, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWest() {
                        walk(Direction.WEST, 0, 0, 0, 0.5F, -1, 0, 0, 0.5, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0.5, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0.5, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0.5, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0.5, jumpWalkBlocks(0, 0, 0));
                    }
                }
            }
        }

        @Nested
        class PartialWidth {
            @Nested
            class PartialHeight {
                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez, double eOffset, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 0.6, 1.95, x, y, z, yo, ex, ey, ez, eOffset, false, pos);
                }

                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez, double eOffset, boolean intermediate, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 0.6, 1.95, x, y, z, yo, ex, ey, ez, eOffset, intermediate,
                            pos);
                }

                @Nested
                class FullBlocks {
                    private static SolidPos[] flatWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                                full(x, y, z - 1)};
                    }

                    private static SolidPos[] jumpWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), full(x + 1, y + 1, z), full(x - 1, y + 1, z),
                                full(x, y + 1, z + 1), full(x, y + 1, z - 1)};
                    }

                    @Test
                    void straightWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkNorthOffset() {
                        walk(Direction.NORTH, 0, 1, 0, 0.5F, 0, 1, -1, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEastOffset() {
                        walk(Direction.EAST, 0, 1, 0, 0.5F, 1, 1, 0, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouthOffset() {
                        walk(Direction.SOUTH, 0, 1, 0, 0.5F, 0, 1, 1, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWestOffset() {
                        walk(Direction.WEST, 0, 1, 0, 0.5F, -1, 1, 0, 0, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0.5F, 0, 2, -1, 0, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0.5F, 1, 2, 0, 0, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0.5F, 0, 2, 1, 0, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0.5F, -1, 2, 0, 0, jumpWalkBlocks(0, 0, 0));
                    }
                }

                @Nested
                class HalfBlocks {
                    private static SolidPos[] flatWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), solid(LOWER_HALF_BLOCK, x + 1, y, z),
                                solid(LOWER_HALF_BLOCK, x - 1, y, z), solid(LOWER_HALF_BLOCK, x, y, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y, z - 1)};
                    }

                    private static SolidPos[] intersectPartial(int x, int y, int z, Solid solid) {
                        return new SolidPos[] {full(x, y, z), solid(solid, x, y + 1, z), full(x + 1, y, z),
                                full(x - 1, y, z), full(x, y, z + 1), full(x, y, z - 1)};
                    }

                    private static SolidPos[] jumpWalkBlocks(int x, int y, int z) {
                        return new SolidPos[] {full(x, y, z), solid(LOWER_HALF_BLOCK, x + 1, y + 1, z),
                                solid(LOWER_HALF_BLOCK, x - 1, y + 1, z), solid(LOWER_HALF_BLOCK, x, y + 1, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y + 1, z - 1)};
                    }

                    private static SolidPos[] stepUpFromSlabBlocks(int x, int y, int z) {
                        return new SolidPos[] {solid(LOWER_HALF_BLOCK, x, y, z), full(x + 1, y, z), full(x - 1, y, z),
                                full(x, y, z + 1), full(x, y, z - 1)};
                    }

                    private static SolidPos[] jumpUpFromSlabBlocks(int x, int y, int z) {
                        return new SolidPos[] {solid(LOWER_HALF_BLOCK, x, y, z), full(x + 1, y, z), full(x - 1, y, z),
                                full(x, y, z + 1), full(x, y, z - 1), solid(LOWER_HALF_BLOCK, x + 1, y + 1, z),
                                solid(LOWER_HALF_BLOCK, x - 1, y + 1, z), solid(LOWER_HALF_BLOCK, x, y + 1, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y + 1, z - 1)};
                    }

                    private static SolidPos[] jumpUpFromSlabBlocked(int x, int y, int z) {
                        return new SolidPos[] {solid(LOWER_HALF_BLOCK, x, y, z), full(x + 1, y, z), full(x - 1, y, z),
                                full(x, y, z + 1), full(x, y, z - 1), solid(LOWER_HALF_BLOCK, x + 1, y + 1, z),
                                solid(LOWER_HALF_BLOCK, x - 1, y + 1, z), solid(LOWER_HALF_BLOCK, x, y + 1, z + 1),
                                solid(LOWER_HALF_BLOCK, x, y + 1, z - 1), solid(UPPER_HALF_BLOCK, x, y + 2, z)};
                    }

                    @Test
                    void jumpUpFromSlabBlockedNorth() {
                        noWalk(Direction.NORTH, 0.6, 1.95, 0, 0, 0, 0.5F, jumpUpFromSlabBlocked(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabBlockedEast() {
                        noWalk(Direction.EAST, 0.6, 1.95, 0, 0, 0, 0.5F, jumpUpFromSlabBlocked(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabBlockedSouth() {
                        noWalk(Direction.SOUTH, 0.6, 1.95, 0, 0, 0, 0.5F, jumpUpFromSlabBlocked(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabBlockedWest() {
                        noWalk(Direction.WEST, 0.6, 1.95, 0, 0, 0, 0.5F, jumpUpFromSlabBlocked(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabNorth() {
                        walk(Direction.NORTH, 0, 0, 0, 0.5F, 0, 1, -1, 0.5, jumpUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabEast() {
                        walk(Direction.EAST, 0, 0, 0, 0.5F, 1, 1, 0, 0.5, jumpUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabSouth() {
                        walk(Direction.SOUTH, 0, 0, 0, 0.5F, 0, 1, 1, 0.5, jumpUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpUpFromSlabWest() {
                        walk(Direction.WEST, 0, 0, 0, 0.5F, -1, 1, 0, 0.5, jumpUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void stepUpFromSlabNorth() {
                        walk(Direction.NORTH, 0, 0, 0, 0.5F, 0, 1, -1, 0, stepUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void stepUpFromSlabEast() {
                        walk(Direction.EAST, 0, 0, 0, 0.5F, 1, 1, 0, 0, stepUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void stepUpFromSlabSouth() {
                        walk(Direction.SOUTH, 0, 0, 0, 0.5F, 0, 1, 1, 0, stepUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void stepUpFromSlabWest() {
                        walk(Direction.WEST, 0, 0, 0, 0.5F, -1, 1, 0, 0, stepUpFromSlabBlocks(0, 0, 0));
                    }

                    @Test
                    void walkNorthMissSouthPartialBlock() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void walkEastMissSouthPartialBlock() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void walkWestMissSouthPartialBlock() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void walkSouthMissNorthPartialBlock() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void walkEastMissNorthPartialBlock() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void walkWestMissNorthPartialBlock() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void walkSouthMissEastPartialBlock() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void walkNorthMissEastPartialBlock() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void walkWestMissEastPartialBlock() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void walkNorthMissWestPartialBlock() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void walkEastMissWestPartialBlock() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void walkSouthMissWestPartialBlock() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0, intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void walkNorthIntersectPartialBlock() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0, true,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_NORTH));
                    }

                    @Test
                    void walkEastIntersectPartialBlock() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0, true,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_EAST));
                    }

                    @Test
                    void walkSouthIntersectPartialBlock() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0, true,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_SOUTH));
                    }

                    @Test
                    void walkWestIntersectPartialBlock() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0, true,
                                intersectPartial(0, 0, 0, PARTIAL_BLOCK_WEST));
                    }

                    @Test
                    void straightWalkNorth() {
                        walk(Direction.NORTH, 0, 0, 0, 0.5F, 0, 0, -1, 0.5, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkEast() {
                        walk(Direction.EAST, 0, 0, 0, 0.5F, 1, 0, 0, 0.5, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkSouth() {
                        walk(Direction.SOUTH, 0, 0, 0, 0.5F, 0, 0, 1, 0.5, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void straightWalkWest() {
                        walk(Direction.WEST, 0, 0, 0, 0.5F, -1, 0, 0, 0.5, flatWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkNorth() {
                        walk(Direction.NORTH, 0, 1, 0, 0, 0, 1, -1, 0.5, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkEast() {
                        walk(Direction.EAST, 0, 1, 0, 0, 1, 1, 0, 0.5, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkSouth() {
                        walk(Direction.SOUTH, 0, 1, 0, 0, 0, 1, 1, 0.5, jumpWalkBlocks(0, 0, 0));
                    }

                    @Test
                    void jumpWalkWest() {
                        walk(Direction.WEST, 0, 1, 0, 0, -1, 1, 0, 0.5, jumpWalkBlocks(0, 0, 0));
                    }
                }
            }
        }

        @Nested
        class LargePartialWidth {
            @Nested
            class FullHeight {
                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez, double eOffset, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 2, 2, x, y, z, yo, ex, ey, ez, eOffset, false, pos);
                }

                private static void walk(Direction direction, int x, int y, int z, float yo, int ex, int ey, int ez, double eOffset, boolean intermediate, SolidPos... pos) {
                    BasicNodeSnapperTest.walk(direction, 2, 2, x, y, z, yo, ex, ey, ez, eOffset, intermediate, pos);
                }

                private static void noWalk(Direction direction, int x, int y, int z, float yo, SolidPos... pos) {
                    BasicNodeSnapperTest.noWalk(direction, 2, 2, x, y, z, yo, pos);
                }

                private static SolidPos[] intersectPartial(int x, int y, int z, Solid solid, Direction direction) {
                    return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                            full(x, y, z - 1), solid(solid, x + direction.x, y + 1, z + direction.z)};
                }

                private static SolidPos[] blockedBySlabsPartial(int x, int y, int z, Direction direction) {
                    return new SolidPos[] {full(x, y, z), full(x + direction.x, y, z + direction.z),
                            full(x + 2 * direction.x, y, z + 2 * direction.z),
                            solid(LOWER_HALF_BLOCK, x + 2 * direction.x, y + 1, z + 2 * direction.z),
                            solid(UPPER_HALF_BLOCK, x + 2 * direction.x, y + 2, z + 2 * direction.z)};
                }

                @Test
                void walkNorthBlockedBySlabsPartial() {
                    noWalk(Direction.NORTH, 0, 1, 0, 0, blockedBySlabsPartial(0, 0, 0, Direction.NORTH));
                }

                @Test
                void walkEastBlockedBySlabsPartial() {
                    noWalk(Direction.EAST, 0, 1, 0, 0, blockedBySlabsPartial(0, 0, 0, Direction.EAST));
                }

                @Test
                void walkSouthBlockedBySlabsPartial() {
                    noWalk(Direction.SOUTH, 0, 1, 0, 0, blockedBySlabsPartial(0, 0, 0, Direction.SOUTH));
                }

                @Test
                void walkWestBlockedBySlabsPartial() {
                    noWalk(Direction.WEST, 0, 1, 0, 0, blockedBySlabsPartial(0, 0, 0, Direction.WEST));
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
    class CheckDiagonal {
        public static final Solid PARTIAL_SOLID = Solid.of(Bounds3D.immutable(0.2, 0, 0.2, 0.6, 1, 0.6));

        public static void checkDiagonal(double width, double height, int x, int y, int z, Direction first, Direction second, float nodeOffset, boolean shouldMiss, SolidPos... solids) {
            BasicNodeSnapper snapper = make(width, height, 0, 0, EPSILON, solids);
            boolean miss = snapper.checkDiagonal(x, y, z, x + first.x + second.x, z + first.z + second.z, nodeOffset);
            if (shouldMiss) {
                assertTrue(miss, "snapper reported a diagonal hit when a miss was expected");
            } else {
                assertFalse(miss, "snapper reported a diagonal miss when a hit was expected");
            }
        }

        public static SolidPos[] clipFullSolids(int x, int y, int z, Direction clip) {
            return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                    full(x, y, z - 1), full(x + clip.x, y + 1, z + clip.z)};
        }

        public static SolidPos[] clipPartialSolids(int x, int y, int z, Direction clip) {
            return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                    full(x, y, z - 1), solid(PARTIAL_SOLID, x + clip.x, y + 1, z + clip.z)};
        }

        public static SolidPos[] clipTinySolids(int x, int y, int z, Direction clip) {
            return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                    full(x, y, z - 1), solid(SMALL_CENTRAL_SOLID, x + clip.x, y + 1, z + clip.z)};
        }

        @Test
        void noSolidsNorthEast() {
            checkDiagonal(1, 1, 0, 0, 0, Direction.NORTH, Direction.EAST, 0, true);
        }

        @Test
        void noSolidsSouthEast() {
            checkDiagonal(1, 1, 0, 0, 0, Direction.SOUTH, Direction.EAST, 0, true);
        }

        @Test
        void noSolidsSouthWest() {
            checkDiagonal(1, 1, 0, 0, 0, Direction.SOUTH, Direction.WEST, 0, true);
        }

        @Test
        void noSolidsNorthWest() {
            checkDiagonal(1, 1, 0, 0, 0, Direction.NORTH, Direction.WEST, 0, true);
        }

        @Nested
        class Full {
            @Test
            void clipNorthNorthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.EAST, 0, false,
                        clipFullSolids(0, 0, 0, Direction.NORTH));
            }

            @Test
            void clipEastNorthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.EAST, 0, false,
                        clipFullSolids(0, 0, 0, Direction.EAST));
            }

            @Test
            void clipSouthSouthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.EAST, 0, false,
                        clipFullSolids(0, 0, 0, Direction.SOUTH));
            }

            @Test
            void clipEastSouthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.EAST, 0, false,
                        clipFullSolids(0, 0, 0, Direction.EAST));
            }

            @Test
            void clipSouthSouthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.WEST, 0, false,
                        clipFullSolids(0, 0, 0, Direction.SOUTH));
            }

            @Test
            void clipWestSouthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.WEST, 0, false,
                        clipFullSolids(0, 0, 0, Direction.WEST));
            }

            @Test
            void clipNorthNorthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.WEST, 0, false,
                        clipFullSolids(0, 0, 0, Direction.NORTH));
            }

            @Test
            void clipWestNorthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.WEST, 0, false,
                        clipFullSolids(0, 0, 0, Direction.WEST));
            }
        }

        @Nested
        class Partial {
            @Test
            void clipNorthNorthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.EAST, 0, false,
                        clipPartialSolids(0, 0, 0, Direction.NORTH));
            }

            @Test
            void clipEastNorthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.EAST, 0, false,
                        clipPartialSolids(0, 0, 0, Direction.EAST));
            }

            @Test
            void clipSouthSouthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.EAST, 0, false,
                        clipPartialSolids(0, 0, 0, Direction.SOUTH));
            }

            @Test
            void clipEastSouthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.EAST, 0, false,
                        clipPartialSolids(0, 0, 0, Direction.EAST));
            }

            @Test
            void clipSouthSouthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.WEST, 0, false,
                        clipPartialSolids(0, 0, 0, Direction.SOUTH));
            }

            @Test
            void clipWestSouthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.WEST, 0, false,
                        clipPartialSolids(0, 0, 0, Direction.WEST));
            }

            @Test
            void clipNorthNorthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.WEST, 0, false,
                        clipPartialSolids(0, 0, 0, Direction.NORTH));
            }

            @Test
            void clipWestNorthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.WEST, 0, false,
                        clipPartialSolids(0, 0, 0, Direction.WEST));
            }
        }

        @Nested
        class Tiny {
            @Test
            void clipNorthNorthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.EAST, 0, true,
                        clipTinySolids(0, 0, 0, Direction.NORTH));
            }

            @Test
            void clipEastNorthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.EAST, 0, true,
                        clipTinySolids(0, 0, 0, Direction.EAST));
            }

            @Test
            void clipSouthSouthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.EAST, 0, true,
                        clipTinySolids(0, 0, 0, Direction.SOUTH));
            }

            @Test
            void clipEastSouthEast() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.EAST, 0, true,
                        clipTinySolids(0, 0, 0, Direction.EAST));
            }

            @Test
            void clipSouthSouthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.WEST, 0, true,
                        clipTinySolids(0, 0, 0, Direction.SOUTH));
            }

            @Test
            void clipWestSouthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.SOUTH, Direction.WEST, 0, true,
                        clipTinySolids(0, 0, 0, Direction.WEST));
            }

            @Test
            void clipNorthNorthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.WEST, 0, true,
                        clipTinySolids(0, 0, 0, Direction.NORTH));
            }

            @Test
            void clipWestNorthWest() {
                checkDiagonal(0.5, 1, 0, 1, 0, Direction.NORTH, Direction.WEST, 0, true,
                        clipTinySolids(0, 0, 0, Direction.WEST));
            }
        }
    }

    @Nested
    class CheckInitial {
        public static void checkInitial(double x, double y, double z, int tx, int ty, int tz, double width, double height, float eHeight, SolidPos... solids) {
            BasicNodeSnapper snapper = make(width, height, 16, 0, EPSILON, solids);
            float res = snapper.checkInitial(x, y, z, tx, ty, tz);
            assertEquals(eHeight, res, "unexpected target height");
        }

        public static SolidPos[] noCollisionBelow(int x, int y, int z) {
            return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                    full(x, y, z - 1)};
        }

        public static SolidPos[] smallCentralSolid(int x, int y, int z) {
            return new SolidPos[] {full(x, y, z), solid(SMALL_CENTRAL_SOLID, x, y + 1, z)};
        }

        public static SolidPos[] smallUpperLeftSolid(int x, int y, int z) {
            return new SolidPos[] {full(x, y, z), solid(SMALL_UPPER_LEFT_SOLID, x, y + 1, z)};
        }

        @Test
        void smallBoundsNoCollision() {
            checkInitial(0.6, 1, 0.6, 0, 1, 0, 0.3, 2, 0, noCollisionBelow(0, 0, 0));
        }

        @Test
        void sameBoundsDiagonalCollision() {
            checkInitial(0.2, 1, 0.2, 0, 1, 0, 0.2, 2, Float.NaN, smallCentralSolid(0, 0, 0));
        }

        @Test
        void sameBoundsMissDiagonal() {
            checkInitial(0.2, 1, 0.2, 0, 1, 0, 0.2, 2, 0, smallUpperLeftSolid(0, 0, 0));
        }

        @Test
        void sameBoundsFallCenterSolid() {
            checkInitial(0.2, 10, 0.2, 0, 1, 0, 0.2, 2, Float.NaN, smallCentralSolid(0, 0, 0));
        }
    }

    @Nested
    class Ranges {
        public static SolidPos[] fullBlockSqueeze(int x, int y, int z) {
            return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                    full(x, y, z - 1), full(x + 1, y + 1, z + 1), full(x - 1, y + 1, z + 1), full(x + 1, y + 1, z - 1),
                    full(x - 1, y + 1, z - 1)};
        }

        public static SolidPos[] nearlyFullBlockSqueeze(int x, int y, int z) {
            return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                    full(x, y, z - 1), solid(NEARLY_FULL_SOLID, x + 1, y + 1, z + 1),
                    solid(NEARLY_FULL_SOLID, x - 1, y + 1, z + 1), solid(NEARLY_FULL_SOLID, x + 1, y + 1, z - 1),
                    solid(NEARLY_FULL_SOLID, x - 1, y + 1, z - 1)};
        }

        public static SolidPos[] stairsSqueeze(int x, int y, int z) {
            return new SolidPos[] {full(x, y, z), full(x + 1, y, z), full(x - 1, y, z), full(x, y, z + 1),
                    full(x, y, z - 1), solid(STAIRS, x + 1, y + 1, z + 1), solid(STAIRS, x - 1, y + 1, z + 1),
                    solid(STAIRS, x + 1, y + 1, z - 1), solid(STAIRS, x - 1, y + 1, z - 1)};
        }

        public static SolidPos[] solidOverlapping(Solid solid, int x, int y, int z) {
            return new SolidPos[] {
                    full(x, y, z),
                    solid(solid, x, y + 1, z),
                    full(x + 1, y, z),
                    full(x - 1, y, z),
                    full(x, y, z + 1),
                    full(x, y, z - 1),
            };
        }

        public static SolidPos[] fullOverlapping(int x, int y, int z) {
            return solidOverlapping(Solid.FULL, x, y, z);
        }

        public static void genWidths(double start, double end, double inc, DoubleConsumer consumer) {
            double s = Math.min(start, end);
            double e = Math.max(start, end);

            for (double width = s; width <= e; width += inc) {
                consumer.accept(width);
            }
        }

        public static void genSubBlockPositions(int x, int y, int z, double width, double inc, Vec3DConsumer consumer) {
            double hWidth = width / 2;

            double startX = x + hWidth + EPSILON;
            double startZ = z + hWidth + EPSILON;

            double endX = (x + 1) - hWidth - EPSILON;
            double endZ = (z + 1) - hWidth - EPSILON;

            for (double sx = startX; sx <= endX; sx += inc) {
                for (double sz = startZ; sz <= endZ; sz += inc) {
                    consumer.consume(sx, y, sz);
                }
            }
        }

        public static void checkManyInitial(int x, int y, int z, Direction direction, double wStart, double wEnd,
                double height,
                float eHeight, SolidPos... solids) {
            int tx = x + direction.x;
            int ty = y + direction.y;
            int tz = z + direction.z;

            genWidths(wStart, wEnd,  0.01, w -> {
                BasicNodeSnapper snapper = make(w, height, 16, 0, EPSILON, solids);
                genSubBlockPositions(x, y, z, w, 0.01, (cx, cy, cz) -> {
                    double distance = Vec3D.distanceSquared(cx, cy, cz, tx + 0.5, cy, cz + 0.5);
                    if (distance > 1) {
                        return;
                    }

                    float res = snapper.checkInitial(cx, cy, cz, tx, ty, tz);
                    assertEquals(eHeight, res,
                            () -> "target height: starting from " + Vec3D.immutable(cx, cy, cz) + " and going to " +
                                    Vec3I.immutable(tx, ty, tz) + " for agent of width " + w);
                });
            });
        }

        public static void snapMany(int x, int y, int z, Direction direction, double wStart, double wEnd, double height,
                float yOffset, float eOffset, boolean intermediate, SolidPos... solids) {

            genWidths(wStart, wEnd, 0.01, w -> {
                walk(direction, w, height, x, y, z, yOffset, x + direction.x, y + direction.y,
                        z + direction.z, eOffset, intermediate, solids);
            });
        }

        public interface Vec3DConsumer {
            void consume(double x, double y, double z);
        }

        @Nested
        class Overlapping {
            @Test
            void fullBlockNorth() {
                snapMany(0, 1, 0, Direction.NORTH, 0.1, 5, 1, 0, 0, false,
                        fullOverlapping(0, 0, 0));
            }

            @Test
            void fullBlockEast() {
                snapMany(0, 1, 0, Direction.EAST, 0.1, 5, 1, 0, 0, false,
                        fullOverlapping(0, 0, 0));
            }

            @Test
            void fullBlockSouth() {
                snapMany(0, 1, 0, Direction.SOUTH, 0.1, 5, 1, 0, 0, false,
                        fullOverlapping(0, 0, 0));
            }

            @Test
            void fullBlockWest() {
                snapMany(0, 1, 0, Direction.WEST, 0.1, 5, 1, 0, 0, false,
                        fullOverlapping(0, 0, 0));
            }

            @Test
            void partialBlockNorth() {
                snapMany(0, 1, 0, Direction.NORTH, 0.9, 5, 1, 0, 0, false,
                        solidOverlapping(PARTIAL_BLOCK_NORTH, 0, 0, 0));
            }

            @Test
            void partialBlockEast() {
                snapMany(0, 1, 0, Direction.EAST, 0.9, 5, 1, 0, 0, false,
                        solidOverlapping(PARTIAL_BLOCK_EAST, 0, 0, 0));
            }

            @Test
            void partialBlockSouth() {
                snapMany(0, 1, 0, Direction.SOUTH, 0.9, 5, 1, 0, 0, false,
                        solidOverlapping(PARTIAL_BLOCK_SOUTH, 0, 0, 0));
            }

            @Test
            void partialBlockWest() {
                snapMany(0, 1, 0, Direction.WEST, 0.9, 5, 1, 0, 0, false,
                        solidOverlapping(PARTIAL_BLOCK_WEST, 0, 0, 0));
            }
        }

        @Nested
        class FullBlocks {
            @Test
            void initialBlockNorth() {
                checkManyInitial(0, 1, 0, Direction.NORTH, 0.1, 1, 1, 0, fullBlockSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockEast() {
                checkManyInitial(0, 1, 0, Direction.EAST, 0.1, 1, 1, 0, fullBlockSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockSouth() {
                checkManyInitial(0, 1, 0, Direction.SOUTH, 0.1, 1, 1, 0, fullBlockSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockWest() {
                checkManyInitial(0, 1, 0, Direction.WEST, 0.1, 1, 1, 0, fullBlockSqueeze(0, 0, 0));
            }
        }

        @Nested
        class NearlyFull {
            @Test
            void initialBlockNorth() {
                checkManyInitial(0, 1, 0, Direction.NORTH, 0.1, 1, 1, 0, nearlyFullBlockSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockEast() {
                checkManyInitial(0, 1, 0, Direction.EAST, 0.1, 1, 1, 0, nearlyFullBlockSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockSouth() {
                checkManyInitial(0, 1, 0, Direction.SOUTH, 0.1, 1, 1, 0, nearlyFullBlockSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockWest() {
                checkManyInitial(0, 1, 0, Direction.WEST, 0.1, 1, 1, 0, nearlyFullBlockSqueeze(0, 0, 0));
            }
        }

        @Nested
        class Stairs {
            @Test
            void initialBlockNorth() {
                checkManyInitial(0, 1, 0, Direction.NORTH, 0.1, 1, 1, 0, stairsSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockEast() {
                checkManyInitial(0, 1, 0, Direction.EAST, 0.1, 1, 1, 0, stairsSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockSouth() {
                checkManyInitial(0, 1, 0, Direction.SOUTH, 0.1, 1, 1, 0, stairsSqueeze(0, 0, 0));
            }

            @Test
            void initialBlockWest() {
                checkManyInitial(0, 1, 0, Direction.WEST, 0.1, 1, 1, 0, stairsSqueeze(0, 0, 0));
            }
        }
    }
}