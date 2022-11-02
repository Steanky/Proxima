package com.github.steanky.proxima;

import com.github.steanky.vector.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BasicAsyncPathfinderTest {
    private static PathSettings settings(int width, int height, int fallTolerance, int jumpHeight,
            @NotNull Space space) {
        return new PathSettings() {
            //using a ThreadLocal HashVec3I2ObjectMap is a very significant performance save
            private static final ThreadLocal<Vec3I2ObjectMap<Node>> THREAD_LOCAL_GRAPH = ThreadLocal.withInitial(() ->
                    new HashVec3I2ObjectMap<>(-100, -100, -100, 100, 100, 100));

            private static final Heuristic HEURISTIC = new Heuristic() {
                @Override
                public float heuristic(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
                    return (float) Vec3I.distanceSquared(fromX, fromY, fromZ, toX, toY, toZ);
                }

                @Override
                public float distance(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
                    return (float) Vec3I.distanceSquared(fromX, fromY, fromZ, toX, toY, toZ);
                }
            };

            private static final Vec3IBiPredicate SUCCESS_PREDICATE = (x1, y1, z1, x2, y2, z2) -> x1 == x2 && y1 == y2
                    && z1 == z2;

            private final Explorer explorer = new DirectionalExplorer(new Direction[] {
                    Direction.NORTH,
                    Direction.EAST,
                    Direction.SOUTH,
                    Direction.WEST
            }, new WalkNodeSnapper(width, height, fallTolerance, jumpHeight, space, Bounds3I.immutable(-100, -100,
                    -100, 200, 200, 200)));

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
                return HEURISTIC;
            }

            @Override
            public @NotNull Vec3I2ObjectMap<Node> graph() {
                return THREAD_LOCAL_GRAPH.get();
            }
        };
    }

    private static Pathfinder pathfinder() {
        int threads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool fjp = new ForkJoinPool(threads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null,
                false, threads, threads, threads, forkJoinPool -> true, 2, TimeUnit.MINUTES);

        return new BasicAsyncPathfinder(fjp, BasicPathOperation::new, 1000000);
    }

    private static PathSettings simpleEnvironment() {
        HashSpace space = new HashSpace(-50, -50, -50, 100, 100, 100);

        for (int x = -50; x < 100; x++) {
            for (int z = -50; z < 100; z++) {
                space.put(x, 0, z, Solid.FULL);
            }
        }

        return settings(1, 1, 1, 1, space);
    }

    @Test
    void overloadSmallFailedPath() {
        HashSpace space = new HashSpace(-100, -100, -100, 100, 100, 100);
        for (Direction direction : Direction.values()) {
            space.put(direction.vector(), Solid.FULL);
        }

        PathSettings settings = settings(1, 1, 4, 1, space);
        Pathfinder pathfinder = pathfinder();

        Set<Vec3I> expected = Set.of(Vec3I.immutable(0, 0, 0));
        IntStream.range(0, 1000000).parallel().forEach(ignored -> {
            PathResult result = assertDoesNotThrow(() -> pathfinder.pathfind(0, 0, 0, 10, 10,
                    10, settings).get());
            assertEquals(expected, result.vectors());
        });
    }

    @Test
    void overloadSimplePath() {
        PathSettings settings = simpleEnvironment();
        Pathfinder pathfinder = pathfinder();

        for (int i = 0; i < 1000000; i++) {
            pathfinder.pathfind(30, 1, 0, 0, 1, 0, settings);
        }

        pathfinder.shutdown();
    }

    @Test
    void simplePath() throws ExecutionException, InterruptedException {
        PathSettings settings = simpleEnvironment();
        Pathfinder pathfinder = pathfinder();

        PathResult result = pathfinder.pathfind(5, 1, 0, 0, 1, 0, settings).get();

        Set<Vec3I> expected = new LinkedHashSet<>(List.of(Vec3I.immutable(5, 1, 0),
                Vec3I.immutable(4, 1, 0), Vec3I.immutable(3, 1, 0), Vec3I.immutable(2, 1, 0),
                Vec3I.immutable(1, 1, 0), Vec3I.immutable(0, 1, 0)));

        assertEquals(expected, result.vectors());
    }
}