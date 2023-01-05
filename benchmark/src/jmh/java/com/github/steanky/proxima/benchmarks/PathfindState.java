package com.github.steanky.proxima.benchmarks;

import com.github.steanky.proxima.*;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.path.BasicAsyncPathfinder;
import com.github.steanky.proxima.path.BasicPathOperation;
import com.github.steanky.proxima.path.PathSettings;
import com.github.steanky.proxima.path.Pathfinder;
import com.github.steanky.proxima.snapper.WalkNodeSnapper;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.ConcurrentCachingSpace;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.*;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class PathfindState {
    public Pathfinder pathfinder;
    public PathSettings settings;

    @Setup(Level.Iteration)
    public void setUp() {
        pathfinder = pathfinder();
        settings = synchronizedEnvironment();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        pathfinder.shutdown();
        settings = null;
    }

    private static Pathfinder pathfinder() {
        int threads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool fjp = new ForkJoinPool(threads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null,
                false, threads, threads, threads, forkJoinPool -> true, 2, TimeUnit.MINUTES);

        return new BasicAsyncPathfinder(fjp, BasicPathOperation::new, 1000000);
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

        return settings(1, 1, 1, 1, space, bounds);
    }

    private static PathSettings settings(int width, int height, int fallTolerance, int jumpHeight,
            @NotNull Space space, Bounds3I searchArea) {
        return new PathSettings() {
            //using a ThreadLocal HashVec3I2ObjectMap is a very significant performance save
            private final ThreadLocal<Vec3I2ObjectMap<Node>> THREAD_LOCAL_GRAPH = ThreadLocal.withInitial(() ->
                    new HashVec3I2ObjectMap<>(searchArea.originX(), searchArea.originX(), searchArea.originZ(),
                            searchArea.lengthX(), searchArea.lengthY(), searchArea.lengthZ()));

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

            private final Explorer explorer = new WalkExplorer(new WalkNodeSnapper(width, height, fallTolerance,
                    jumpHeight, space, searchArea, 1E-6));

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
}
