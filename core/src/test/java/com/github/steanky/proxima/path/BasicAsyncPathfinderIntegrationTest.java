package com.github.steanky.proxima.path;

import com.github.steanky.proxima.Direction;
import com.github.steanky.proxima.Heuristic;
import com.github.steanky.proxima.PathLimiter;
import com.github.steanky.proxima.explorer.Explorer;
import com.github.steanky.proxima.explorer.WalkExplorer;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.node.NodeProcessor;
import com.github.steanky.proxima.snapper.BasicNodeSnapper;
import com.github.steanky.proxima.snapper.NodeSnapper;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.ConcurrentCachingSpace;
import com.github.steanky.proxima.space.HashSpace;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class BasicAsyncPathfinderIntegrationTest {
    private static PathSettings settings(int width, int height, int fallTolerance, int jumpHeight, @NotNull Space space, Bounds3I searchArea, Function<NodeSnapper, NodeProcessor> processorFunction) {
        return new PathSettings() {
            private static final Vec3IBiPredicate SUCCESS_PREDICATE =
                    (x1, y1, z1, x2, y2, z2) -> x1 == x2 && y1 == y2 && z1 == z2;
            //using a ThreadLocal HashVec3I2ObjectMap is a very significant performance save
            private final ThreadLocal<Vec3I2ObjectMap<Node>> THREAD_LOCAL_GRAPH =
                    ThreadLocal.withInitial(() -> new HashVec3I2ObjectMap<>(searchArea));
            private final NodeSnapper snapper =
                    new BasicNodeSnapper(space, width, height, fallTolerance, jumpHeight, 1E-6);

            private final Explorer explorer = new WalkExplorer(snapper, PathLimiter.inBounds(searchArea));

            private final NodeProcessor processor = processorFunction.apply(snapper);

            @Override
            public @NotNull Vec3IBiPredicate successPredicate() {
                return SUCCESS_PREDICATE;
            }

            @Override
            public @NotNull Explorer explorer() {
                return explorer;
            }

            @Override
            public @NotNull Heuristic heuristic() {
                return Heuristic.DISTANCE_SQUARED;
            }

            @Override
            public @NotNull Vec3I2ObjectMap<Node> graph() {
                return THREAD_LOCAL_GRAPH.get();
            }

            @Override
            public @NotNull NodeProcessor nodeProcessor() {
                return processor;
            }
        };
    }

    private static Pathfinder pathfinder() {
        int threads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool fjp =
                new ForkJoinPool(threads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, false, threads,
                        threads, threads, forkJoinPool -> true, 2, TimeUnit.MINUTES);

        return new BasicAsyncPathfinder(fjp, BasicPathOperation::new, 1000000);
    }

    private static PathSettings simpleEnvironment() {
        HashSpace space = new HashSpace(-50, -50, -50, 100, 100, 100);

        for (int x = -50; x < 100; x++) {
            for (int z = -50; z < 100; z++) {
                space.put(x, 0, z, Solid.FULL);
            }
        }

        return settings(1, 1, 1, 1, space, Bounds3I.immutable(-100, -100, -100, 200, 200, 200),
                (ignored) -> NodeProcessor.NO_CHANGE);
    }

    private static PathSettings hugeEnvironment() {
        HashSpace space = new HashSpace(0, 0, 0, 1000, 4, 1000);

        for (int x = 0; x < 1000; x++) {
            for (int z = 0; z < 1000; z++) {
                space.put(x, 0, z, Solid.FULL);
            }
        }

        return settings(1, 1, 1, 1, space, Bounds3I.immutable(0, 0, 0, 1000, 4, 1000),
                (ignored) -> NodeProcessor.NO_CHANGE);
    }

    private static PathSettings hugeEnvironmentWithPartialBlocks() {
        Solid stairs = Solid.of(Bounds3D.immutable(0, 0, 0, 1, 0.5, 1), Bounds3D.immutable(0, 0.5, 0.5, 0.5, 0.5, 1));

        HashSpace space = new HashSpace(0, 0, 0, 1000, 4, 1000);
        for (int x = 0; x < 1000; x++) {
            for (int z = 0; z < 1000; z++) {
                space.put(x, 0, z, stairs);
            }
        }

        return settings(1, 1, 1, 1, space, Bounds3I.immutable(0, 0, 0, 1000, 4, 1000),
                (ignored) -> NodeProcessor.NO_CHANGE);
    }

    private static PathSettings synchronizedEnvironment() {
        Bounds3I bounds = Bounds3I.immutable(0, 0, 0, 1000, 4, 1000);

        Space space = new ConcurrentCachingSpace() {
            @Override
            public @NotNull Solid loadSolid(int x, int y, int z) {
                if (y == 0) {
                    return Solid.FULL;
                }

                return Solid.EMPTY;
            }
        };

        return settings(1, 1, 1, 1, space, bounds, (ignored) -> NodeProcessor.NO_CHANGE);
    }

    private static PathSettings synchronizedDiagonalProcessingEnvironment() {
        Bounds3I bounds = Bounds3I.immutable(0, 0, 0, 1000, 4, 1000);

        Space space = new ConcurrentCachingSpace() {
            @Override
            public @NotNull Solid loadSolid(int x, int y, int z) {
                if (y == 0) {
                    return Solid.FULL;
                }

                return Solid.EMPTY;
            }
        };

        return settings(1, 1, 1, 1, space, bounds, NodeProcessor::createDiagonals);
    }

    @Test
    void smallFailedPath() {
        HashSpace space = new HashSpace(-100, -100, -100, 100, 100, 100);
        for (Direction direction : Direction.values()) {
            space.put(direction.vector(), Solid.FULL);
        }

        space.put(0, -1, 0, Solid.FULL);
        space.put(0, 1, 0, Solid.FULL);

        PathSettings settings = settings(1, 1, 4, 1, space, Bounds3I.immutable(-100, -100, -100, 200, 200, 200),
                (ignored) -> NodeProcessor.NO_CHANGE);
        Pathfinder pathfinder = pathfinder();

        List<Vec3I> expected = List.of(Vec3I.immutable(0, 0, 0));

        PathResult result = assertDoesNotThrow(
                () -> pathfinder.pathfind(0.5, 0, 0.5, PathTarget.coordinate(10, 10, 10), settings).get());

        assertPathEquals(expected, false, result);
    }

    @Test
    void unsuccessfulPathWithCorrection() {
        HashSpace space = new HashSpace(-100, -100, -100, 100, 100, 100);
        for (Direction direction : Direction.values()) {
            space.put(direction.vector(), Solid.FULL);
        }

        space.put(0, -1, 0, Solid.FULL);
        space.put(0, 1, 0, Solid.FULL);

        PathSettings settings = settings(1, 1, 4, 1, space, Bounds3I.immutable(-100, -100, -100, 200, 200, 200),
                (ignored) -> NodeProcessor.NO_CHANGE);
        Pathfinder pathfinder = pathfinder();

        PathResult result = assertDoesNotThrow(
                () -> pathfinder.pathfind(0, 0, 0, PathTarget.coordinate(5, 5, 5), settings).get());

        assertFalse(result.isSuccessful());
        Node node = result.head();
        assertNotNull(node);
        assertTrue(node.positionEquals(Vec3I.ORIGIN));
    }

    @Test
    void overloadSimplePath() {
        PathSettings settings = simpleEnvironment();
        Pathfinder pathfinder = pathfinder();

        for (int i = 0; i < 1000000; i++) {
            pathfinder.pathfind(30, 1, 0, PathTarget.coordinate(0, 1, 0), settings);
        }

        pathfinder.shutdown();
    }

    @Test
    void simplePath()
    throws ExecutionException, InterruptedException {
        PathSettings settings = simpleEnvironment();
        Pathfinder pathfinder = pathfinder();

        PathResult result = pathfinder.pathfind(5, 1, 0, PathTarget.coordinate(0, 1, 0), settings).get();

        List<Vec3I> expected = List.of(Vec3I.immutable(5, 1, 0), Vec3I.immutable(4, 1, 0), Vec3I.immutable(3, 1, 0),
                Vec3I.immutable(2, 1, 0), Vec3I.immutable(1, 1, 0), Vec3I.immutable(0, 1, 0));

        assertPathEquals(expected, true, result);
    }

    @Test
    void hugePath() {
        PathSettings settings = hugeEnvironment();
        Pathfinder pathfinder = pathfinder();

        for (int i = 0; i < 1000; i++) {
            pathfinder.pathfind(0, 1, 0, PathTarget.coordinate(0, 1, 0), settings);
        }

        pathfinder.shutdown();
    }

    @Test
    void hugePathWithPartialBlocks() {
        PathSettings settings = hugeEnvironmentWithPartialBlocks();
        Pathfinder pathfinder = pathfinder();

        for (int i = 0; i < 1000; i++) {
            pathfinder.pathfind(0, 1, 0, PathTarget.coordinate(900, 1, 900), settings);
        }

        pathfinder.shutdown();
    }

    @Test
    void synchronizedHugePath() {
        PathSettings settings = synchronizedEnvironment();
        Pathfinder pathfinder = pathfinder();

        for (int i = 0; i < 1000; i++) {
            pathfinder.pathfind(0, 1, 0, PathTarget.coordinate(900, 1, 900), settings);
        }

        pathfinder.shutdown();
    }

    @Test
    void synchronizedHugePathAdjustingDiagonals() {
        PathSettings settings = synchronizedDiagonalProcessingEnvironment();
        Pathfinder pathfinder = pathfinder();

        for (int i = 0; i < 1000; i++) {
            pathfinder.pathfind(0, 1, 0, PathTarget.coordinate(900, 1, 900), settings);
        }

        pathfinder.shutdown();
    }

    private void assertPathEquals(List<Vec3I> expected, boolean success, PathResult result) {
        if (success) {
            assertTrue(result.isSuccessful(), "expected successful result");
        } else {
            assertFalse(result.isSuccessful(), "expected failed result");
        }

        Node head = result.head();
        assertNotNull(head, "head was null");

        List<Node> nodes = head.toList();
        assertEquals(expected.size(), nodes.size(), "path length mismatch");

        for (int i = 0; i < expected.size(); i++) {
            Vec3I vec = expected.get(i);
            Node node = nodes.get(i);

            assertEquals(vec.x(), node.x, "node " + i + " x-coordinate");
            assertEquals(vec.y(), node.y, "node " + i + " y-coordinate");
            assertEquals(vec.z(), node.z, "node " + i + " z-coordinate");
        }
    }
}