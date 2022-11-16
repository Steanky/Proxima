package com.github.steanky.proxima;

import com.github.steanky.proxima.node.Node;
import com.github.steanky.vector.Vec3I;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    @Test
    void invertToList() {
        Node second = new Node(0, 0, 0, 0, 0, null, 0);
        Node first = new Node(0, 0, 1, 0, 0, second, 0);

        Set<Vec3I> positions = first.reverseToVectorSet();
        Iterator<Vec3I> itr = positions.iterator();
        assertEquals(Vec3I.ORIGIN, itr.next());
        assertEquals(Vec3I.immutable(0, 0, 1), itr.next());
    }
}