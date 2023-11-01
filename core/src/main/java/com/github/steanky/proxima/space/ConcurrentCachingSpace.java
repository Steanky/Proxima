package com.github.steanky.proxima.space;

import com.github.steanky.proxima.solid.Solid;
import com.github.steanky.vector.Vec3I;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.StampedLock;

/**
 * A thread-safe {@link Space} implementation. Must be subclassed by a class which provides actual {@link Solid}
 * instances from some source, for example a Minecraft chunk.
 * <p>
 * This class is designed to minimize lock contention in the common case of heavily concurrent reads and relatively few
 * writes. It is often the case that simply <i>reading</i> {@link Solid} objects is poorly performant or not conducive
 * to massively parallel reads. The {@link ConcurrentCachingSpace#loadSolid(int, int, int)} method is expected to
 * utilize algorithms like this; it may be slow to read from, or the data may be synchronized in such a way that
 * concurrent reads are not possible. Solids returned from this method are cached in a parallelism-friendly data
 * structure that allows as much concurrent access as possible.
 */
public abstract class ConcurrentCachingSpace implements Space {
    private final StampedLock lock;
    private final Long2ObjectOpenHashMap<Chunk> cache;

    private final int minimumY;

    public ConcurrentCachingSpace(int minimumY) {
        this.lock = new StampedLock();
        this.cache = new Long2ObjectOpenHashMap<>();
        this.minimumY = minimumY;
    }

    public ConcurrentCachingSpace() {
        this(-32);
    }

    private Chunk getChunk(long chunkKey) {
        long read = lock.tryOptimisticRead();
        if (lock.validate(read)) {
            Chunk chunk = null;
            boolean exception = false;
            try {
                chunk = cache.get(chunkKey);
            }
            catch (Throwable ignored) {
                exception = true;
            }

            if (!exception && lock.validate(read)) {
                return chunk;
            }
        }

        read = lock.readLock();
        try {
            return cache.get(chunkKey);
        }
        finally {
            lock.unlockRead(read);
        }
    }

    private void updateExistingOrNewChunk(Chunk chunk, long chunkKey, int blockKey, Solid solidToWrite, boolean force) {
        boolean removing = solidToWrite == null;

        if (chunk != null) {
            //write to our suggested chunk
            //may call updateExistingOrNewChunk again, with force set to true and a null chunk, if the update fails
            ensureUpdate(chunk, chunkKey, blockKey, solidToWrite);
            return;
        }

        long cacheStamp = force ? lock.writeLock() : lock.readLock();
        try {
            //may be non-null
            //if so, don't create a new chunk, remove write lock, and add the solid to the chunk
            //otherwise, if the chunk is null, create a new chunk, add our initial solid to it, and put it in the map
            Chunk otherChunk = cache.get(chunkKey);
            if (otherChunk != null) {
                //another thread added a chunk - use it
                chunk = otherChunk;
            } else if (!removing) {
                //acquire write lock as we're going to add a new chunk to the cache
                cacheStamp = upgradeToWriteLock(lock, cacheStamp);

                //we might have just finished waiting on another thread to add a chunk
                otherChunk = cache.get(chunkKey);

                if (otherChunk == null) {
                    //create a new chunk, add our solid to it, and put it in the cache
                    //we don't need to write-lock on the newly-created chunk at all this way
                    chunk = new Chunk();
                    chunk.map.put(blockKey, solidToWrite);

                    cache.put(chunkKey, chunk);
                    return;
                }

                chunk = otherChunk;
            }
            else { //otherChunk is null, but we are removing, don't create a chunk just to remove from it!
                return;
            }

            if (force) {
                //slow path: write to chunk occurs under cache write lock
                //write only returns false when the chunk being written to has been removed
                //the chunk is only marked removed after it is removed from the cache, done under cache write-lock
                //hit when calling recursively if a previous attempt to remove/write to a chunk fails
                if (removing) {
                    if (chunk.remove(blockKey) == Chunk.RemovalState.CHUNK_REMOVED) {
                        throw new IllegalStateException();
                    }
                }
                else if(!chunk.write(blockKey, solidToWrite)) {
                    throw new IllegalStateException();
                }

                return;
            }
        } finally {
            lock.unlock(cacheStamp);
        }

        ensureUpdate(chunk, chunkKey, blockKey, solidToWrite);
    }

    private void ensureUpdate(Chunk chunk, long chunkKey, int blockKey, Solid solidToWrite) {
        if (solidToWrite == null) {
            //don't do anything else if case NEITHER
            switch (chunk.remove(blockKey)) {
                case CHUNK_REMOVED -> //re-update with force this time!
                        updateExistingOrNewChunk(null, chunkKey, blockKey, null, true);
                case MAP_EMPTY -> //remove from cache if we're actually empty
                        removeFromCache(chunk, chunkKey, false);
            }
        }
        else if (!chunk.write(blockKey, solidToWrite)) {
            updateExistingOrNewChunk(null, chunkKey, blockKey, solidToWrite, true);
        }
    }

    private void removeFromCache(Chunk chunk, long chunkKey, boolean force) {
        long cacheWrite = lock.writeLock();

        //will block writes to the chunk while it is undergoing removal
        long chunkWrite = chunk.lock.writeLock();
        try {
            if (chunk.removed || (!force && !chunk.map.isEmpty())) {
                return;
            }

            //removals (or additions) to cache only occur when under write lock
            if (cache.remove(chunkKey) != chunk) {
                throw new IllegalStateException();
            }

            chunk.removed = true;
        }
        finally {
            lock.unlockWrite(cacheWrite);
            chunk.lock.unlockWrite(chunkWrite);
        }
    }

    @Override
    public final @Nullable Solid solidAt(int x, int y, int z) {
        long chunkKey = Chunk.key(x, z);
        int blockKey = Chunk.relative(x, y, z, minimumY);

        Chunk chunk = getChunk(chunkKey);
        Solid solid;
        if (chunk == null) {
            solid = loadSolid(x, y, z);
            if (solid == null) {
                return null;
            }

            updateExistingOrNewChunk(null, chunkKey, blockKey, solid, false);
            return solid;
        }

        solid = chunk.read(blockKey);
        if (solid != null) {
            return solid;
        }

        solid = loadSolid(x, y, z);
        if (solid == null) {
            return null;
        }

        updateExistingOrNewChunk(chunk, chunkKey, blockKey, solid, false);
        return solid;
    }

    @Override
    public final @Nullable Solid solidAt(@NotNull Vec3I vec) {
        return solidAt(vec.x(), vec.y(), vec.z());
    }

    /**
     * Updates the solid at the given position. If the solid is null, any solid that was cached at that location will be
     * removed, to be re-computed when needed.
     *
     * @param x     the x-coordinate of the solid to update
     * @param y     the y-coordinate of the solid to update
     * @param z     the z-coordinate of the solid to update
     * @param solid the new solid, or null to remove any cached solid (if present)
     */
    public void updateSolid(int x, int y, int z, @Nullable Solid solid) {
        updateExistingOrNewChunk(null, Chunk.key(x, z), Chunk.relative(x, y, z, minimumY), solid, false);
    }

    private static long upgradeToWriteLock(StampedLock stampedLock, long heldStamp) {
        if (stampedLock.isWriteLocked()) {
            return heldStamp;
        }

        stampedLock.unlockRead(heldStamp);
        return stampedLock.writeLock();
    }

    /**
     * Clears the cache, reducing it to a state similar to when it was first initialized.
     */
    public void clearCache() {
        long cacheWrite = lock.writeLock();
        try {
            ObjectIterator<Long2ObjectMap.Entry<Chunk>> entrySetIterator = cache.long2ObjectEntrySet().fastIterator();

            while (entrySetIterator.hasNext()) {
                Chunk chunk = entrySetIterator.next().getValue();

                long chunkWrite = chunk.lock.writeLock();

                try {
                    entrySetIterator.remove();
                    chunk.removed = true;
                }
                finally {
                    chunk.lock.unlockWrite(chunkWrite);
                }
            }
        } finally {
            lock.unlockWrite(cacheWrite);
        }
    }

    /**
     * Removes a cached chunk, if it exists.
     * @param x the x-coordinate (chunk)
     * @param z the z-coordinate (chunk)
     */
    public void clearChunk(int x, int z) {
        long key = Chunk.keyFromChunk(x, z);

        Chunk chunk = getChunk(key);

        //removed will never be set 'false' after set to true
        if (chunk == null || chunk.removed) {
            return;
        }

        //force = true to remove the chunk even if it has blocks in it!
        removeFromCache(chunk, key, true);
    }

    /**
     * Loads a solid, which will be cached in this space until it is invalidated. This method is called by
     * {@link ConcurrentCachingSpace#solidAt(int, int, int)} when it encounters a cache miss.
     * <p>
     * This method may be called concurrently by two or more threads. However, this class will cache any solids it
     * returns. Future requests for solids which have been cached will use a concurrency-friendly data structure that is
     * designed to minimize lock contention.
     * <p>
     * This method may return null to indicate that no solid can be found at the given location, and no solid will be
     * cached in this case.
     *
     * @param x the x-coordinate of the solid to load
     * @param y the y-coordinate of the solid to load
     * @param z the z-coordinate of the solid to load
     *
     * @return a Solid object, or null to indicate inability to load at this position
     */
    public abstract @Nullable Solid loadSolid(int x, int y, int z);

    private static final class Chunk {
        private enum RemovalState {
            CHUNK_REMOVED,
            MAP_EMPTY,
            NEITHER
        }

        private final Int2ObjectMap<Solid> map;
        private final StampedLock lock;

        //must ONLY be set under write lock of both cache and this chunk
        //once set to true, will never be set to 'false' again
        private volatile boolean removed;

        private Chunk() {
            this.map = new Int2ObjectOpenHashMap<>();
            this.lock = new StampedLock();
        }

        private static long key(int x, int z) {
            int hi = x >> 4;
            int lo = z >> 4;
            return keyFromChunk(hi, lo);
        }

        private static long keyFromChunk(int x, int z) {
            return (((long) x) << 32) | (z & 0xFFFF_FFFFL);
        }

        private static int relative(int x, int y, int z, int minY) {
            return ((x & 15) << 15) | (((y - minY) & 2047) << 4) | (z & 15);
        }

        @SuppressWarnings("DuplicatedCode")
        private Solid read(int key) {
            long read = lock.readLock();
            try {
                //we tried to optimistically read too many times; we need to do a full read
                return map.get(key);
            } finally {
                lock.unlockRead(read);
            }
        }

        //this method returns true when the operation succeeded, false otherwise
        //the operation can only "fail" if this chunk has been removed
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean write(int key, Solid solid) {
            long chunkStamp = lock.writeLock();

            try {
                if (this.removed) {
                    return false;
                }

                map.put(key, solid);
            } finally {
                lock.unlockWrite(chunkStamp);
            }

            return true;
        }

        private RemovalState remove(int key) {
            long chunkWrite = lock.writeLock();

            try {
                if (this.removed) {
                    return RemovalState.CHUNK_REMOVED;
                }

                map.remove(key);
                return map.isEmpty() ? RemovalState.MAP_EMPTY : RemovalState.NEITHER;
            } finally {
                lock.unlockWrite(chunkWrite);
            }
        }
    }
}
