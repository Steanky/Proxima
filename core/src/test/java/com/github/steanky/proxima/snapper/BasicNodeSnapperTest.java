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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.DoubleConsumer;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
class BasicNodeSnapperTest {
    private static final String METHOD_PATH = "com.github.steanky.proxima.snapper.BasicNodeSnapperTest#coordinates";

    private static final Arguments[] vectors;

    static {
        vectors = new Arguments[125];

        int n = 0;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -2; k <= 2; k++) {
                    vectors[n++] = Arguments.of(i, j, k);
                }
            }
        }
    }

    public static @NotNull Arguments[] coordinates() {
        return vectors;
    }

    public static final double EPSILON = 1E-6;

    public static final Solid LOWER_HALF_BLOCK = Solid.of(Bounds3D.immutable(0, 0, 0, 1, 0.5, 1));

    public static final Solid UPPER_HALF_BLOCK = Solid.of(Bounds3D.immutable(0, 0.5, 0, 1, 0.5, 1));

    public static final Solid PARTIAL_BLOCK_NORTH = Solid.of(Bounds3D.immutable(0, 0, 0, 1, 1, 0.0625));

    public static final Solid PARTIAL_BLOCK_SOUTH = Solid.of(Bounds3D.immutable(0, 0, 0.9375, 1, 1, 0.0625));

    public static final Solid PARTIAL_BLOCK_EAST = Solid.of(Bounds3D.immutable(0.9375, 0, 0, 0.0625, 1, 1));

    public static final Solid PARTIAL_BLOCK_WEST = Solid.of(Bounds3D.immutable(0, 0, 0, 0.0625, 1, 1));

    public static final Solid SMALL_CENTRAL_SOLID = Solid.of(Bounds3D.immutable(0.45, 0, 0.45, 0.1, 1, 0.1));

    public static final Solid TINY_CENTRAL_SOLID = Solid.of(Bounds3D.immutable(0.45, 0, 0.45, 0.1, 0.5, 0.1));

    public static final Solid SMALL_UPPER_LEFT_SOLID = Solid.of(Bounds3D.immutable(0, 0, 0.7, 0.1, 1, 0.1));

    public static final Solid NEARLY_FULL_SOLID = Solid.of(Bounds3D.immutable(0.1, 0, 0.1, 0.8, 1, 0.8));

    public static final Solid CENTERED_HALF_BLOCK = Solid.of(Bounds3D.immutable(0.25, 0, 0.25, 0.5, 5, 0.5));

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

    private static Node node(int x, int y, int z, float yOffset) {
        return new Node(x, y, z, 0, 0, yOffset);
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

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0, stairsBelow(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0, stairsBelow(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0, stairsBelow(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0, stairsBelow(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightJumpNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 2, z - 1, 0, stairsAround(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightJumpEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 2, z, 0, stairsAround(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightJumpSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 2, z + 1, 0, stairsAround(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightJumpWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 2, z, 0, stairsAround(x, y, z));
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

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpNorthThenFall(int x, int y, int z) {
                        noWalk(Direction.NORTH, 0.5, 1, x, y + 1, z, 0,
                                intermediateJumpThenFall(x, y, z, PARTIAL_BLOCK_NORTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpSouthThenFall(int x, int y, int z) {
                        noWalk(Direction.SOUTH, 0.5, 1, x, y + 1, z, 0,
                                intermediateJumpThenFall(x, y, z, PARTIAL_BLOCK_SOUTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpEastThenFall(int x, int y, int z) {
                        noWalk(Direction.EAST, 0.5, 1, x, y + 1, z, 0,
                                intermediateJumpThenFall(x, y, z, PARTIAL_BLOCK_EAST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWestThenFall(int x, int y, int z) {
                        noWalk(Direction.WEST, 0.5, 1, x, y + 1, z, 0,
                                intermediateJumpThenFall(x, y, z, PARTIAL_BLOCK_WEST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void fallSingleWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y, z - 1, 0, fallWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void fallSingleWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y, z, 0, fallWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void fallSingleWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y, z + 1, 0, fallWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void fallSingleWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y, z, 0, fallWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void enclosedWalkNorth(int x, int y, int z) {
                        noWalk(Direction.NORTH, 1, 1, x, y, z, 0.0F, enclosedBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void enclosedWalkEast(int x, int y, int z) {
                        noWalk(Direction.EAST, 1, 1, x, y, z, 0.0F, enclosedBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void enclosedWalkSouth(int x, int y, int z) {
                        noWalk(Direction.SOUTH, 1, 1, x, y, z, 0.0F, enclosedBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void enclosedWalkWest(int x, int y, int z) {
                        noWalk(Direction.WEST, 1, 1, x, y, z, 0.0F, enclosedBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkNorthOffset(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0.5F, x, y + 1, z - 1, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkEastOffset(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0.5F, x + 1, y + 1, z, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkSouthOffset(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0.5F, x, y + 1, z + 1, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkWestOffset(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0.5F, x - 1, y + 1, z, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0.5F, x, y + 2, z - 1, 0, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0.5F, x + 1, y + 2, z, 0, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0.5F, x, y + 2, z + 1, 0, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0.5F, x - 1, y + 2, z, 0, jumpWalkBlocks(x, y, z));
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

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y, z, 0.5F, x, y, z - 1, 0.5, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y, z, 0.5F, x + 1, y, z, 0.5, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y, z, 0.5F, x, y, z + 1, 0.5, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y, z, 0.5F, x - 1, y, z, 0.5, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0.5, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0.5, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0.5, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0.5, jumpWalkBlocks(x, y, z));
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

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkNorthOffset(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0.5F, x, y + 1, z - 1, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkEastOffset(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0.5F, x + 1, y + 1, z, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkSouthOffset(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0.5F, x, y + 1, z + 1, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkWestOffset(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0.5F, x - 1, y + 1, z, 0, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0.5F, x, y + 2, z - 1, 0, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0.5F, x + 1, y + 2, z, 0, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0.5F, x, y + 2, z + 1, 0, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0.5F, x - 1, y + 2, z, 0, jumpWalkBlocks(x, y, z));
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

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpUpFromSlabBlockedNorth(int x, int y, int z) {
                        noWalk(Direction.NORTH, 0.6, 1.95, x, y, z, 0.5F, jumpUpFromSlabBlocked(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpUpFromSlabBlockedEast(int x, int y, int z) {
                        noWalk(Direction.EAST, 0.6, 1.95, x, y, z, 0.5F, jumpUpFromSlabBlocked(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpUpFromSlabBlockedSouth(int x, int y, int z) {
                        noWalk(Direction.SOUTH, 0.6, 1.95, x, y, z, 0.5F, jumpUpFromSlabBlocked(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpUpFromSlabBlockedWest(int x, int y, int z) {
                        noWalk(Direction.WEST, 0.6, 1.95, x, y, z, 0.5F, jumpUpFromSlabBlocked(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpUpFromSlabNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y, z, 0.5F, x, y + 1, z - 1, 0.5, jumpUpFromSlabBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpUpFromSlabEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y, z, 0.5F, x + 1, y + 1, z, 0.5, jumpUpFromSlabBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpUpFromSlabSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y, z, 0.5F, x, y + 1, z + 1, 0.5, jumpUpFromSlabBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpUpFromSlabWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y, z, 0.5F, x - 1, y + 1, z, 0.5, jumpUpFromSlabBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void stepUpFromSlabNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y, z, 0.5F, x, y + 1, z - 1, 0, stepUpFromSlabBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void stepUpFromSlabEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y, z, 0.5F, x + 1, y + 1, z, 0, stepUpFromSlabBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void stepUpFromSlabSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y, z, 0.5F, x, y + 1, z + 1, 0, stepUpFromSlabBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void stepUpFromSlabWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y, z, 0.5F, x - 1, y + 1, z, 0, stepUpFromSlabBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkNorthMissSouthPartialBlock(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_SOUTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkEastMissSouthPartialBlock(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_SOUTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkWestMissSouthPartialBlock(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_SOUTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkSouthMissNorthPartialBlock(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_NORTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkEastMissNorthPartialBlock(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_NORTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkWestMissNorthPartialBlock(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_NORTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkSouthMissEastPartialBlock(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_EAST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkNorthMissEastPartialBlock(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_EAST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkWestMissEastPartialBlock(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_EAST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkNorthMissWestPartialBlock(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_WEST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkEastMissWestPartialBlock(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_WEST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkSouthMissWestPartialBlock(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0, intersectPartial(x, y, z, PARTIAL_BLOCK_WEST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkNorthIntersectPartialBlock(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0, true,
                                intersectPartial(x, y, z, PARTIAL_BLOCK_NORTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkEastIntersectPartialBlock(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0, true,
                                intersectPartial(x, y, z, PARTIAL_BLOCK_EAST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkSouthIntersectPartialBlock(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0, true,
                                intersectPartial(x, y, z, PARTIAL_BLOCK_SOUTH));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void walkWestIntersectPartialBlock(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0, true,
                                intersectPartial(x, y, z, PARTIAL_BLOCK_WEST));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y, z, 0.5F, x, y, z - 1, 0.5, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y, z, 0.5F, x + 1, y, z, 0.5, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y, z, 0.5F, x, y, z + 1, 0.5, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void straightWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y, z, 0.5F, x - 1, y, z, 0.5, flatWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkNorth(int x, int y, int z) {
                        walk(Direction.NORTH, x, y + 1, z, 0, x, y + 1, z - 1, 0.5, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkEast(int x, int y, int z) {
                        walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 1, z, 0.5, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkSouth(int x, int y, int z) {
                        walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 1, z + 1, 0.5, jumpWalkBlocks(x, y, z));
                    }

                    @ParameterizedTest
                    @MethodSource(METHOD_PATH)
                    void jumpWalkWest(int x, int y, int z) {
                        walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 1, z, 0.5, jumpWalkBlocks(x, y, z));
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

                @ParameterizedTest
                @MethodSource(METHOD_PATH)
                void walkNorthBlockedBySlabsPartial(int x, int y, int z) {
                    noWalk(Direction.NORTH, x, y + 1, z, 0, blockedBySlabsPartial(x, y, z, Direction.NORTH));
                }

                @ParameterizedTest
                @MethodSource(METHOD_PATH)
                void walkEastBlockedBySlabsPartial(int x, int y, int z) {
                    noWalk(Direction.EAST, x, y + 1, z, 0, blockedBySlabsPartial(x, y, z, Direction.EAST));
                }

                @ParameterizedTest
                @MethodSource(METHOD_PATH)
                void walkSouthBlockedBySlabsPartial(int x, int y, int z) {
                    noWalk(Direction.SOUTH, x, y + 1, z, 0, blockedBySlabsPartial(x, y, z, Direction.SOUTH));
                }

                @ParameterizedTest
                @MethodSource(METHOD_PATH)
                void walkWestBlockedBySlabsPartial(int x, int y, int z) {
                    noWalk(Direction.WEST, x, y + 1, z, 0, blockedBySlabsPartial(x, y, z, Direction.WEST));
                }

                @ParameterizedTest
                @MethodSource(METHOD_PATH)
                void walkNorthIntersectPartial(int x, int y, int z) {
                    walk(Direction.NORTH, x, y + 1, z, 0, x, y + 2, z - 1, 0,
                            intersectPartial(x, y, z, PARTIAL_BLOCK_NORTH, Direction.NORTH));
                }

                @ParameterizedTest
                @MethodSource(METHOD_PATH)
                void walkEastIntersectPartial(int x, int y, int z) {
                    walk(Direction.EAST, x, y + 1, z, 0, x + 1, y + 2, z, 0,
                            intersectPartial(x, y, z, PARTIAL_BLOCK_EAST, Direction.EAST));
                }

                @ParameterizedTest
                @MethodSource(METHOD_PATH)
                void walkSouthIntersectPartial(int x, int y, int z) {
                    walk(Direction.SOUTH, x, y + 1, z, 0, x, y + 2, z + 1, 0,
                            intersectPartial(x, y, z, PARTIAL_BLOCK_SOUTH, Direction.SOUTH));
                }

                @ParameterizedTest
                @MethodSource(METHOD_PATH)
                void walkWestIntersectPartial(int x, int y, int z) {
                    walk(Direction.WEST, x, y + 1, z, 0, x - 1, y + 2, z, 0,
                            intersectPartial(x, y, z, PARTIAL_BLOCK_WEST, Direction.WEST));
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

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void noSolidsNorthEast(int x, int y, int z) {
            checkDiagonal(1, 1, x, y, z, Direction.NORTH, Direction.EAST, 0, true);
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void noSolidsSouthEast(int x, int y, int z) {
            checkDiagonal(1, 1, x, y, z, Direction.SOUTH, Direction.EAST, 0, true);
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void noSolidsSouthWest(int x, int y, int z) {
            checkDiagonal(1, 1, x, y, z, Direction.SOUTH, Direction.WEST, 0, true);
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void noSolidsNorthWest(int x, int y, int z) {
            checkDiagonal(1, 1, x, y, z, Direction.NORTH, Direction.WEST, 0, true);
        }

        @Nested
        class Full {
            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipNorthNorthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.EAST, 0, false,
                        clipFullSolids(x, y, z, Direction.NORTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipEastNorthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.EAST, 0, false,
                        clipFullSolids(x, y, z, Direction.EAST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipSouthSouthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.EAST, 0, false,
                        clipFullSolids(x, y, z, Direction.SOUTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipEastSouthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.EAST, 0, false,
                        clipFullSolids(x, y, z, Direction.EAST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipSouthSouthWest(int x, int y, int z) {
                x = 0;
                y =0;
                z = 0;
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.WEST, 0, false,
                        clipFullSolids(x, y, z, Direction.SOUTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipWestSouthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.WEST, 0, false,
                        clipFullSolids(x, y, z, Direction.WEST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipNorthNorthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.WEST, 0, false,
                        clipFullSolids(x, y, z, Direction.NORTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipWestNorthWest(int x, int y, int z) {
                x = 0;
                y = 0;
                z = 0;
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.WEST, 0, false,
                        clipFullSolids(x, y, z, Direction.WEST));
            }
        }

        @Nested
        class Partial {
            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipNorthNorthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.EAST, 0, false,
                        clipPartialSolids(x, y, z, Direction.NORTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipEastNorthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.EAST, 0, false,
                        clipPartialSolids(x, y, z, Direction.EAST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipSouthSouthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.EAST, 0, false,
                        clipPartialSolids(x, y, z, Direction.SOUTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipEastSouthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.EAST, 0, false,
                        clipPartialSolids(x, y, z, Direction.EAST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipSouthSouthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.WEST, 0, false,
                        clipPartialSolids(x, y, z, Direction.SOUTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipWestSouthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.WEST, 0, false,
                        clipPartialSolids(x, y, z, Direction.WEST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipNorthNorthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.WEST, 0, false,
                        clipPartialSolids(x, y, z, Direction.NORTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipWestNorthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.WEST, 0, false,
                        clipPartialSolids(x, y, z, Direction.WEST));
            }
        }

        @Nested
        class Tiny {
            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipNorthNorthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.EAST, 0, true,
                        clipTinySolids(x, y, z, Direction.NORTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipEastNorthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.EAST, 0, true,
                        clipTinySolids(x, y, z, Direction.EAST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipSouthSouthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.EAST, 0, true,
                        clipTinySolids(x, y, z, Direction.SOUTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipEastSouthEast(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.EAST, 0, true,
                        clipTinySolids(x, y, z, Direction.EAST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipSouthSouthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.WEST, 0, true,
                        clipTinySolids(x, y, z, Direction.SOUTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipWestSouthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.SOUTH, Direction.WEST, 0, true,
                        clipTinySolids(x, y, z, Direction.WEST));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipNorthNorthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.WEST, 0, true,
                        clipTinySolids(x, y, z, Direction.NORTH));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void clipWestNorthWest(int x, int y, int z) {
                checkDiagonal(0.5, 1, x, y + 1, z, Direction.NORTH, Direction.WEST, 0, true,
                        clipTinySolids(x, y, z, Direction.WEST));
            }
        }
    }

    @Nested
    class CheckInitial {
        public static void checkInitial(double x, double y, double z, int tx, int ty, int tz, double width, double height, double jumpHeight, float eOffset, int eBlock, SolidPos... solids) {
            BasicNodeSnapper snapper = make(width, height, 16, jumpHeight, EPSILON, solids);
            long res = snapper.checkInitial(x, y, z, tx, ty, tz);

            if (Float.isNaN(eOffset)) {
                assertEquals(NodeSnapper.FAIL, res, "wasn't NodeSnapper.FAIL");
            }
            else {
                assertNotEquals(NodeSnapper.FAIL, res, "was NodeSnapper.FAIL");

                assertEquals(eBlock, NodeSnapper.blockHeight(res), "block height");
                assertEquals(eOffset, NodeSnapper.blockOffset(res), "block offset");
            }
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

        public static SolidPos[] tinyCentralSolidBlocked(int x, int y, int z) {
            return new SolidPos[]{ full(x, y, z), solid(TINY_CENTRAL_SOLID, x, y + 1, z), full(x, y + 3, z) };
        }

        public static SolidPos[] tinyCentralSolid(int x, int y, int z) {
            return new SolidPos[]{ full(x, y, z), solid(TINY_CENTRAL_SOLID, x, y + 1, z) };
        }

        public static SolidPos[] surroundingSolids(int x, int y, int z) {
            return new SolidPos[]{ full(x, y - 1, z), full(x, y + 1, z), full(x + 1, y, z),
                    full(x - 1, y, z), full(x, y, z + 1), full(x, y, z - 1) };
        }

        public static SolidPos[] centeredSolid(int x, int y, int z) {
            return new SolidPos[]{ full(x, y - 1, z), solid(CENTERED_HALF_BLOCK, x, y, z), full(x + 1, y - 1, z),
                    full(x - 1, y - 1, z), full(x, y - 1, z + 1), full(x, y - 1, z - 1)};
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void smallBoundsNoCollision(int x, int y, int z) {
            checkInitial(x + 0.6, y + 1, z + 0.6, x, y + 1, z, 0.3, 2, 0, 0,
                    y + 1, noCollisionBelow(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void sameBoundsDiagonalCollision(int x, int y, int z) {
            checkInitial(x + 0.2, y + 1, z + 0.2, x, y + 1, z, 0.2, 2, 0, Float.NaN,
                    0, smallCentralSolid(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void sameBoundsMissDiagonal(int x, int y, int z) {
            checkInitial(x + 0.2, y + 1, z + 0.2, x, y + 1, z, 0.2, 2, 0, 0,
                    y + 1, smallUpperLeftSolid(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void sameBoundsFallCenterSolid(int x, int y, int z) {
            checkInitial(x + 0.2, y + 10, z + 0.2, x, y + 1, z, 0.2, 2, 0, Float.NaN,
                    0, smallCentralSolid(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void sameBoundsBlockedCenterSolid(int x, int y, int z) {
            checkInitial(x + 0.9, y + 1, z + 0.5, x, y + 1, z, 0.2, 1.9, 1, Float.NaN,
                    0, tinyCentralSolidBlocked(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void sameBoundsUnblockedCenterSolid(int x, int y, int z) {
            checkInitial(x + 0.9, y + 1, z + 0.5, x, y + 1, z, 0.2, 1.9, 1,
                    0.5F, y + 1, tinyCentralSolid(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void diagonalSlotIn(int x, int y, int z) {
            checkInitial(x, y, z, x, y, z, 1, 1, 0, 0, y, surroundingSolids(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void walkAwayFromPartialNorth(int x, int y, int z) {
            x = 0;
            y = 0;
            z = 0;
            checkInitial(x + 0.5, y + 1, z, x, y + 1, z - 1, 0.5, 0.5, 0,
                    0, y, centeredSolid(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void walkAwayFromPartialEast(int x, int y, int z) {
            checkInitial(x + 1, y + 1, z + 0.5, x + 1, y + 1, z, 0.5, 0.5, 0,
                    0, y, centeredSolid(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void walkAwayFromPartialSouth(int x, int y, int z) {
            checkInitial(x + 0.5, y + 1, z + 1, x, y + 1, z + 1, 0.5, 0.5, 0,
                    0, y, centeredSolid(x, y, z));
        }

        @ParameterizedTest
        @MethodSource(METHOD_PATH)
        void walkAwayFromPartialWest(int x, int y, int z) {
            checkInitial(x, y + 1, z + 0.5, x - 1, y + 1, z, 0.5, 0.5, 0,
                    0, y, centeredSolid(x, y, z));
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
                    double distance = Vec3D.distanceSquared(cx, cy, cz, tx + 0.5, ty, tz + 0.5);
                    if (distance > 1) {
                        return;
                    }

                    long res = snapper.checkInitial(cx, cy, cz, tx, ty, tz);
                    assertEquals(eHeight, NodeSnapper.blockOffset(res),
                            () -> "target height: starting from " + Vec3D.immutable(cx, cy, cz) + " and going to " +
                                    Vec3I.immutable(tx, ty, tz) + " for agent of width " + w);
                });
            });
        }

        public static void snapMany(int x, int y, int z, Direction direction, double wStart, double wEnd, double height,
                float yOffset, float eOffset, boolean intermediate, SolidPos... solids) {

            genWidths(wStart, wEnd, 0.01, w -> walk(direction, w, height, x, y, z, yOffset, x + direction.x, y + direction.y,
                    z + direction.z, eOffset, intermediate, solids));
        }

        public interface Vec3DConsumer {
            void consume(double x, double y, double z);
        }

        @Nested
        class Overlapping {
            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void fullBlockNorth(int x, int y, int z) {
                snapMany(x, y + 1, z, Direction.NORTH, 0.1, 5, 1, 0, 0, false,
                        fullOverlapping(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void fullBlockEast(int x, int y, int z) {
                snapMany(x, y + 1, z, Direction.EAST, 0.1, 5, 1, 0, 0, false,
                        fullOverlapping(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void fullBlockSouth(int x, int y, int z) {
                snapMany(x, y + 1, z, Direction.SOUTH, 0.1, 5, 1, 0, 0, false,
                        fullOverlapping(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void fullBlockWest(int x, int y, int z) {
                snapMany(x, y + 1, z, Direction.WEST, 0.1, 5, 1, 0, 0, false,
                        fullOverlapping(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void partialBlockNorth(int x, int y, int z) {
                snapMany(x, y + 1, z, Direction.NORTH, 0.9, 5, 1, 0, 0, false,
                        solidOverlapping(PARTIAL_BLOCK_NORTH, x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void partialBlockEast(int x, int y, int z) {
                snapMany(x, y + 1, z, Direction.EAST, 0.9, 5, 1, 0, 0, false,
                        solidOverlapping(PARTIAL_BLOCK_EAST, x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void partialBlockSouth(int x, int y, int z) {
                snapMany(x, y + 1, z, Direction.SOUTH, 0.9, 5, 1, 0, 0, false,
                        solidOverlapping(PARTIAL_BLOCK_SOUTH, x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void partialBlockWest(int x, int y, int z) {
                snapMany(x, y + 1, z, Direction.WEST, 0.9, 5, 1, 0, 0, false,
                        solidOverlapping(PARTIAL_BLOCK_WEST, x, y, z));
            }
        }

        @Nested
        class FullBlocks {
            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockNorth(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.NORTH, 0.1, 1, 1, 0, fullBlockSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockEast(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.EAST, 0.1, 1, 1, 0, fullBlockSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockSouth(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.SOUTH, 0.1, 1, 1, 0, fullBlockSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockWest(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.WEST, 0.1, 1, 1, 0, fullBlockSqueeze(x, y, z));
            }
        }

        @Nested
        class NearlyFull {
            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockNorth(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.NORTH, 0.1, 1, 1, 0, nearlyFullBlockSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockEast(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.EAST, 0.1, 1, 1, 0, nearlyFullBlockSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockSouth(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.SOUTH, 0.1, 1, 1, 0, nearlyFullBlockSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockWest(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.WEST, 0.1, 1, 1, 0, nearlyFullBlockSqueeze(x, y, z));
            }
        }

        @Nested
        class Stairs {
            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockNorth(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.NORTH, 0.1, 1, 1, 0, stairsSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockEast(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.EAST, 0.1, 1, 1, 0, stairsSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockSouth(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.SOUTH, 0.1, 1, 1, 0, stairsSqueeze(x, y, z));
            }

            @ParameterizedTest
            @MethodSource(METHOD_PATH)
            void initialBlockWest(int x, int y, int z) {
                checkManyInitial(x, y + 1, z, Direction.WEST, 0.1, 1, 1, 0, stairsSqueeze(x, y, z));
            }
        }
    }
}