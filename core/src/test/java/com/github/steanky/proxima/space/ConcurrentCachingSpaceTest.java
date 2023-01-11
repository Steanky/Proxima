package com.github.steanky.proxima.space;

import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.vector.Bounds3I;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentCachingSpaceTest {
    private static ConcurrentCachingSpace backed(Space other) {
        return new ConcurrentCachingSpace() {
            @Override
            public @NotNull Solid loadSolid(int x, int y, int z) {
                return other.solidAt(x, y, z);
            }
        };
    }

    @Nested
    class SingleThread {
        @Test
        void cachedReads() {
            HashSpace backing = new HashSpace(Bounds3I.immutable(0, 0, 0, 2048, 2048, 2048));
            backing.put(0, 0, 0, Solid.FULL);
            backing.put(100, 100, 10, Solid.FULL);

            ConcurrentCachingSpace space = backed(backing);

            for (int i = 0; i < 10; i++) {
                Solid solid = space.solidAt(0, 0, 0);
                Solid solid2 = space.solidAt(100, 100, 10);

                assertEquals(Solid.FULL, solid);
                assertEquals(Solid.FULL, solid2);
            }

            backing.remove(0, 0, 0);

            //solid should exist in cache but not in the backing map
            assertEquals(Solid.FULL, space.solidAt(0, 0, 0));

            //remove solid from cache
            space.updateSolid(0, 0, 0, null);
            assertEquals(Solid.EMPTY, space.solidAt(0, 0, 0));
        }
    }

    @Nested
    class ManyThreads {
        @Test
        void parallelReads() {
            HashSpace backing = new HashSpace(Bounds3I.immutable(0, 0, 0, 2048, 2048, 2048));
            backing.put(0, 0, 0, Solid.FULL);

            ConcurrentCachingSpace space = backed(backing);

            for (int i = 0; i < 10; i++) {
                ForkJoinPool.commonPool().execute(() -> {
                    for (int x = 0; x < 100; x++) {
                        for (int y = 0; y < 100; y++) {
                            for (int z = 0; z < 100; z++) {
                                Solid solid = space.solidAt(x, y, z);
                                if (x == 0 && y == 0 && z == 0) {
                                    assertEquals(Solid.FULL, solid);
                                }
                                else {
                                    assertEquals(Solid.EMPTY, solid);
                                }
                            }
                        }
                    }
                });

                if (!ForkJoinPool.commonPool().awaitQuiescence(100, TimeUnit.HOURS)) {
                    fail("timeout");
                }
            }
        }

        @Test
        void writeContention() throws InterruptedException {
            HashSpace backing = new HashSpace(Bounds3I.immutable(0, 0, 0, 2048, 2048, 2048));
            backing.put(0, 0, 0, Solid.FULL);

            ConcurrentCachingSpace space = backed(backing);

            Thread writer = new Thread(() -> {
                while (!Thread.interrupted()) {
                    space.updateSolid(0, 0, 0, Solid.FULL);
                }
            });
            writer.start();

            Thread reader = new Thread(() -> {
               while (!Thread.interrupted()) {
                   assertEquals(Solid.FULL, space.solidAt(0, 0, 0));
               }
            });
            reader.start();

            for (int i = 0; i < 10000000; i++) {
                ForkJoinPool.commonPool().execute(() -> {
                    Solid solid = space.solidAt(0, 0, 0);
                    assertEquals(Solid.FULL, solid);
                });
            }

            if (!ForkJoinPool.commonPool().awaitQuiescence(100, TimeUnit.HOURS)) {
                fail("timeout");
            }

            reader.interrupt();
            reader.join();

            writer.interrupt();
            writer.join();
        }
    }
}