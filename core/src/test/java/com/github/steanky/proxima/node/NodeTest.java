package com.github.steanky.proxima.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class NodeTest {
    @Test
    void reverse() {
        Node second = new Node(0, 0, 0, 0, 0, 0);
        Node first = new Node(0, 0, 1, 0, 0, 0);
        first.parent = second;

        Node newHead = first.reverse();

        assertSame(second, newHead);
        assertSame(first, newHead.parent);

        assertNull(first.parent);
    }
}