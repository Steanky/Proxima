package com.github.steanky.proxima.benchmarks;

import com.github.steanky.proxima.Heuristic;
import com.github.steanky.proxima.PathLimiter;
import com.github.steanky.proxima.explorer.Explorer;
import com.github.steanky.proxima.explorer.WalkExplorer;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.node.NodeProcessor;
import com.github.steanky.proxima.path.BasicAsyncPathfinder;
import com.github.steanky.proxima.path.BasicPathOperation;
import com.github.steanky.proxima.path.PathSettings;
import com.github.steanky.proxima.path.Pathfinder;
import com.github.steanky.proxima.snapper.BasicNodeSnapper;
import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.proxima.space.ConcurrentCachingSpace;
import com.github.steanky.proxima.space.Space;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.HashVec3I2ObjectMap;
import com.github.steanky.vector.Vec3I2ObjectMap;
import com.github.steanky.vector.Vec3IBiPredicate;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class PathfindState {
    public Pathfinder pathfinder;
    public PathSettings settings;

    private static Pathfinder pathfinder() {
        int threads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool fjp =
                new ForkJoinPool(threads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, false, threads,
                        threads, threads, forkJoinPool -> true, 2, TimeUnit.MINUTES);

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

    private static PathSettings settings(int width, int height, int fallTolerance, int jumpHeight, @NotNull Space space, Bounds3I searchArea) {
        return new PathSettings() {
            private static final Vec3IBiPredicate SUCCESS_PREDICATE =
                    (x1, y1, z1, x2, y2, z2) -> x1 == x2 && y1 == y2 && z1 == z2;
            //using a ThreadLocal HashVec3I2ObjectMap is a very significant performance save
            private final ThreadLocal<Vec3I2ObjectMap<Node>> THREAD_LOCAL_GRAPH = ThreadLocal.withInitial(
                    () -> new HashVec3I2ObjectMap<>(searchArea.originX(), searchArea.originX(), searchArea.originZ(),
                            searchArea.lengthX(), searchArea.lengthY(), searchArea.lengthZ()));
            private final Explorer explorer =
                    new WalkExplorer(new BasicNodeSnapper(space, width, height, fallTolerance, jumpHeight, 1E-6),
                            PathLimiter.inBounds(searchArea));

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
                return NodeProcessor.NO_CHANGE;
            }
        };
    }

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
}
