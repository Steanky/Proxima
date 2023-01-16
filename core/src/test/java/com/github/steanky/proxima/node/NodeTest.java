package com.github.steanky.proxima.node;

import com.github.steanky.vector.Vec3I;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeTest {
    @Test
    void invertToList() {
        Node second = new Node(0, 0, 0, 0, 0, 0);
        Node first = new Node(0, 0, 1, 0, 0, 0);
        first.parent = second;

        List<Node> positions = first.reverseToNavigationList();
        Iterator<Node> itr = positions.iterator();
        assertTrue(itr.next().positionEquals(Vec3I.ORIGIN));
        assertTrue(itr.next().positionEquals(Vec3I.immutable(0, 0, 1)));
    }
}