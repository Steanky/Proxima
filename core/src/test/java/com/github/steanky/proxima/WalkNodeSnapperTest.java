package com.github.steanky.proxima;

import com.github.steanky.toolkit.function.Wrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalkNodeSnapperTest {
    @Test
    void initialize() {
        Space space = new HashSpace(-10, -10, -10, 20, 20, 20);
        assertDoesNotThrow(() -> new WalkNodeSnapper(1, 2, 16, 1, space));
    }

    @Test
    void canPassEmpty() {
        HashSpace space = new HashSpace(-10, -10, -10, 20, 20, 20);
        space.put(0, 0, 0, Solid.FULL);
        space.put(1, 0, 0, Solid.FULL);

        Node node = new Node(0, 1, 0, 0, 0, null);
        WalkNodeSnapper snapper = new WalkNodeSnapper(1, 2, 4, 1, space);

        Wrapper<Boolean> callIndicator = Wrapper.of(false);
        snapper.snap(Direction.EAST, node, (n, x, y, z) -> {
            callIndicator.set(true);

            assertSame(node, n);
            assertEquals(1, x);
            assertEquals(1, y);
            assertEquals(0, z);
        });

        assertTrue(callIndicator.get(), "NodeHandler was not called");
    }

    @Test
    void testJump() {
        HashSpace space = new HashSpace(-10, -10, -10, 20, 20, 20);
        space.put(0, 0, 0, Solid.FULL);
        space.put(1, 1, 0, Solid.FULL);

        Node node = new Node(0, 1, 0, 0, 0, null);
        WalkNodeSnapper snapper = new WalkNodeSnapper(1, 2, 4, 1, space);

        Wrapper<Boolean> callIndicator = Wrapper.of(false);
        snapper.snap(Direction.EAST, node, (n, x, y, z) -> {
            callIndicator.set(true);

            assertSame(node, n);
            assertEquals(1, x);
            assertEquals(2, y);
            assertEquals(0, z);
        });

        assertTrue(callIndicator.get(), "NodeHandler was not called");
    }
}