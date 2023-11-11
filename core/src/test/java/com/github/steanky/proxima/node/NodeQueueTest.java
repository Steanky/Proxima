package com.github.steanky.proxima.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeQueueTest {
    @Test
    void changed() {
        Node first = new Node(0, 0, 0, 0, 0, 0);
        Node second = new Node(0, 0, 0, 1, 0, 0);

        NodeQueue queue = new NodeQueue();
        queue.enqueue(second);
        queue.enqueue(first);

        assertEquals(2, queue.size());

        assertSame(first, queue.first());
        assertSame(first, queue.dequeue());

        assertSame(second, queue.dequeue());

        assertEquals(0, queue.size());

        queue.enqueue(second);
        queue.enqueue(first);

        first.g = 2;
        queue.enqueueOrUpdate(first);

        assertSame(second, queue.first());
        assertSame(second, queue.dequeue());

        assertEquals(first, queue.dequeue());
    }

    @Test
    void equalInsertionOrder() {
        Node first = new Node(0, 0, 0, 0, 0, 0);
        Node second = new Node(0, 0, 0, 0, 0, 0);

        NodeQueue queue = new NodeQueue();
        queue.enqueue(first);
        queue.enqueue(second);

        assertSame(first, queue.first());
        assertSame(first, queue.dequeue());

        assertSame(second, queue.dequeue());

        assertEquals(0, queue.size());
    }
}