package com.github.steanky.proxima.solid;

import com.github.steanky.proxima.Direction;
import com.github.steanky.toolkit.collection.Containers;
import com.github.steanky.vector.Bounds3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

final class SolidImpl implements Solid {
    private final Bounds3D bounds;
    private final List<Bounds3D> boundsList;
    private final boolean isFull;
    private final boolean isEmpty;

    SolidImpl() {
        this.bounds = null;
        this.boundsList = List.of();
        this.isFull = false;
        this.isEmpty = true;
    }

    SolidImpl(@NotNull Bounds3D first) {
        this.bounds = validate(first.immutable());
        this.boundsList = List.of(this.bounds);
        this.isFull = this.bounds.volume() == 1;
        this.isEmpty = false;
    }

    SolidImpl(@NotNull Bounds3D first, @NotNull Bounds3D second) {
        Bounds3D firstImmutable = first.immutable();
        Bounds3D secondImmutable = second.immutable();

        this.bounds = validate(Bounds3D.enclosingImmutable(firstImmutable, secondImmutable));
        this.boundsList = List.of(firstImmutable, secondImmutable);
        this.isFull = false;
        this.isEmpty = false;
    }

    SolidImpl(@NotNull Bounds3D @NotNull [] bounds) {
        if (bounds.length == 0) {
            throw new IllegalArgumentException("Cannot construct solid with no bounds");
        }

        Bounds3D[] newArray = new Bounds3D[bounds.length];
        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = bounds[i].immutable();
        }

        this.bounds = validate(Bounds3D.enclosingImmutable(newArray));
        this.boundsList = Containers.arrayView(newArray);
        this.isFull = false;
        this.isEmpty = false;
    }

    private static Bounds3D validate(Bounds3D bounds) {
        if (bounds.originX() < 0 || bounds.maxX() > 1 ||
                bounds.originY() < 0 || bounds.maxY() > 1 ||
                bounds.originZ() < 0 || bounds.maxZ() > 1) {
            throw new IllegalArgumentException("invalid solid bounds");
        }

        return bounds;
    }

    @Override
    public Bounds3D bounds() {
        return bounds;
    }

    @Override
    public boolean isFull() {
        return isFull;
    }

    @Override
    public boolean isEmpty() {
        return isEmpty;
    }

    @Override
    public @NotNull @Unmodifiable List<Bounds3D> children() {
        return boundsList;
    }

    @Override
    public long minMaxCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, @NotNull Direction d, double l, double e) {
        return Util.minMaxCollision(this, x, y, z, cx, cy, cz, lx, ly, lz, d, l, e);
    }

    @Override
    public @Nullable Bounds3D closestCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, @NotNull Direction d, double l, double e) {
        return Util.closestCollision(this, x, y, z, cx, cy, cz, lx, ly, lz, d, l, e);
    }

    @Override
    public boolean hasCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, @NotNull Direction d, double l, double e) {
        return Util.hasCollision(this, x, y, z, cx, cy, cz, lx, ly, lz, d, l, e);
    }

    @Override
    public boolean hasCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, double dx, double dy, double dz, double e) {
        return Util.hasCollision(this, x, y, z, cx, cy, cz, lx, ly, lz, dx, dy, dz, e);
    }

    @Override
    public long minMaxCollision(int x, int y, int z, double cx, double cy, double cz, double lx, double ly, double lz, double dx, double dy, double dz, double e) {
        return Util.minMaxCollision(this, x, y, z, cx, cy, cz, lx, ly, lz, dx, dy, dz, e);
    }

    @Override
    public int hashCode() {
        return boundsList.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)  {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof SolidImpl solid) {
            return solid.boundsList.equals(this.boundsList);
        }

        return false;
    }
}
