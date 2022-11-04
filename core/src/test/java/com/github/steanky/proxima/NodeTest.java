package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Vec3I;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    @Test
    void invertToList() {
        Node second = new Node(0, 0, 0, 0, 0, Movement.UNKNOWN, null);
        Node first = new Node(0, 0, 1, 0, 0, Movement.UNKNOWN, second);

        Set<Vec3I> positions = first.reverseToVectorSet();
        assertEquals(Vec3I.ORIGIN, positions.iterator().next());
        assertEquals(Vec3I.immutable(0, 0, 1), positions.iterator().next());
    }
}