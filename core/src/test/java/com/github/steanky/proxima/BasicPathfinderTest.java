package com.github.steanky.proxima;

import com.github.steanky.vector.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

class BasicPathfinderTest {
    private static PathSettings settings(int width, int height, int fallTolerance, int jumpHeight,
            @NotNull Space space) {
        return new PathSettings() {
            //using a ThreadLocal HashVec3I2ObjectMap is a very significant performance save
            private static final ThreadLocal<Vec3I2ObjectMap<Node>> threadLocal = ThreadLocal.withInitial(() ->
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

            private final Explorer explorer = new DirectionalExplorer(new Direction[] {
                    Direction.NORTH,
                    Direction.EAST,
                    Direction.SOUTH,
                    Direction.WEST
            }, new WalkNodeSnapper(width, height, fallTolerance, jumpHeight, space));

            @Override
            public @NotNull Vec3IBiPredicate successPredicate() {
                return (x1, y1, z1, x2, y2, z2) -> x1 == x2 && y1 == y2 && z1 == z2;
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
                return threadLocal.get();
            }
        };
    }

    @Test
    void overloadSmallFailedPath() {
        HashSpace space = new HashSpace(-100, -100, -100, 100, 100, 100);
        for (Direction direction : Direction.values()) {
            space.put(direction.vector(), Solid.FULL);
        }

        PathSettings settings = settings(1, 1, 4, 1, space);
        int threads = Runtime.getRuntime().availableProcessors();
        Pathfinder handler = new BasicPathfinder(new ThreadPoolExecutor(threads, threads,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(65535),
                new ThreadPoolExecutor.CallerRunsPolicy()), BasicPathOperation::new,
                Executors.newSingleThreadScheduledExecutor(), 8192);

        int total = 10000000;
        for (int i = 0; i < total; i++) {
            handler.pathfind(0, 0, 0, 10, 10, 10, settings);
        }
    }

}