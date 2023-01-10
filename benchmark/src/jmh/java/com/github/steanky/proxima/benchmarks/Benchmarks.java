package com.github.steanky.proxima.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;

import java.util.concurrent.ExecutionException;

@Fork(value = 1, warmups = 1)
public class Benchmarks {
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void run(PathfindState state) throws ExecutionException, InterruptedException {
        state.pathfinder.pathfind(0, 1, 0, 100, 1, 100, state.settings).get();
    }
}
