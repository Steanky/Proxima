package com.github.steanky.proxima;

import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.HashVec3I2ObjectMap;
import com.github.steanky.vector.Vec3I;
import com.github.steanky.vector.Vec3I2ObjectMap;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.StampedLock;

/**
 * A thread-safe {@link Space} implementation. Must be subclassed by a class which provides actual {@link Solid}
 * instances from some source, for example a Minecraft chunk.
 */
public abstract class ConcurrentCachingSpace implements Space {
    private static final int WRITE_ACQUIRE_ATTEMPTS = 10;

    private final Vec3I2ObjectMap<Solid> cache;
    private final StampedLock stampedLock;

    public ConcurrentCachingSpace(@NotNull Bounds3I cacheBounds) {
        this.cache = new HashVec3I2ObjectMap<>(cacheBounds);
        this.stampedLock = new StampedLock();
    }

    private long upgradeReadLock(long readStamp) {
        long newStamp;
        int attempts = 0;

        do {
            newStamp = stampedLock.tryConvertToWriteLock(readStamp);
        }
        while (newStamp == 0 && attempts++ < WRITE_ACQUIRE_ATTEMPTS);

        if (newStamp == 0) {
            stampedLock.unlockRead(readStamp);
            newStamp = stampedLock.writeLock();
        }

        return newStamp;
    }

    @Override
    public final @NotNull Solid solidAt(int x, int y, int z) {
        long stamp = stampedLock.readLock();

        Solid solid;
        try {
            solid = cache.get(x, y, z);
        }
        finally {
            stampedLock.unlockRead(stamp);
        }

        if (solid == null) {
            //load the solid first
            //we don't even hold the read lock for this, which is fine
            solid = loadSolid(x, y, z);

            //if we successfully loaded the solid, acquire a write lock
            long writeStamp = stampedLock.writeLock();
            try {
                cache.put(x, y, z, solid);
            }
            finally {
                stampedLock.unlockWrite(writeStamp);
            }
        }

        return solid;
    }

    @Override
    public final @NotNull Solid solidAt(@NotNull Vec3I vec) {
        return solidAt(vec.x(), vec.y(), vec.z());
    }

    public void invalidateCache(int x, int y, int z) {
        long stamp = stampedLock.readLock();

        //we can check containsKey just with the read lock
        //this avoids costly and unnecessary write locks
        if (cache.containsKey(x, y, z)) {
            stamp = upgradeReadLock(stamp);

            try {
                cache.remove(x, y, z);
            }
            finally {
                stampedLock.unlockWrite(stamp);
            }
        }
        else {
            stampedLock.unlockRead(stamp);
        }
    }

    public void clearCache() {
        long stamp = stampedLock.readLock();

        if (!cache.isEmpty()) {
            stamp = upgradeReadLock(stamp);

            try {
                cache.clear();
            }
            finally {
                stampedLock.unlockWrite(stamp);
            }
        }
        else {
            stampedLock.unlockRead(stamp);
        }
    }

    /**
     * Loads a solid, which will be cached in this space until it is invalidated. This method is called by
     * {@link ConcurrentCachingSpace#solidAt(int, int, int)} when it encounters a cache miss.
     * <p>
     * Threads calling this method will typically not hold any write locks. Therefore, implementations should expect
     * that this method will be called concurrently by two or more threads.
     *
     * @param x the x-coordinate of the solid to load
     * @param y the y-coordinate of the solid to load
     * @param z the z-coordinate of the solid to load
     * @return a Solid object
     */
    public abstract @NotNull Solid loadSolid(int x, int y, int z);
}
