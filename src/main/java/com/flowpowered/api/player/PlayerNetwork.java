/*
 * This file is part of Flow Engine, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.api.player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.flowpowered.api.Client;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.geo.reference.ChunkReference;
import com.flowpowered.api.player.reposition.NullRepositionManager;
import com.flowpowered.api.player.reposition.RepositionManager;
import com.flowpowered.engine.geo.chunk.FlowChunk;
import com.flowpowered.engine.network.FlowSession;
import com.flowpowered.engine.network.message.ChunkDataMessage;
import com.flowpowered.engine.network.message.UpdateEntityMessage;
import com.flowpowered.networking.session.Session;

/**
 * The networking behind {@link Player}s. This component holds the {@link Session} which is the connection the Player has to the server.
 * This class also syncs *loaded* chunks to the client. It makes no attempt to load or generate chunks.
 */
public class PlayerNetwork {
    protected static final int CHUNKS_PER_TICK = 50;
    private final FlowSession session;
    /**
     * Chunks that have been given to us to send. May or may not be sent.
     */
    private final Set<ChunkReference> chunks = new HashSet<>();
    /**
     * Chunks that have been sent to the client
     */
    private final Set<ChunkReference> activeChunks = new LinkedHashSet<>();
    private final Set<ChunkReference> chunkSendQueue = new LinkedHashSet<>();
    private final Set<ChunkReference> chunkFreeQueue = new LinkedHashSet<>();

    protected volatile Transform previousTransform = Transform.INVALID;
    private final AtomicReference<RepositionManager> rm = new AtomicReference<>(NullRepositionManager.INSTANCE);

    public PlayerNetwork(FlowSession session) {
        this.session = session;
    }

    /**
     * Returns the {@link Session} representing the connection to the server.
     *
     * @return The session
     */
    public final FlowSession getSession() {
        return session;
    }

    /**
     * Gets the reposition manager that converts local coordinates into remote coordinates
     */
    public RepositionManager getRepositionManager() {
        return rm.get();
    }

    public void setRepositionManager(RepositionManager rm) {
        if (rm == null) {
            this.rm.set(NullRepositionManager.INSTANCE);
        } else {
            this.rm.set(rm);
        }
    }

    public void addChunks(Set<Chunk> chunks) {
        chunkSendQueue.addAll(chunks.stream().map(ChunkReference::new).collect(
                Collectors.toList()));
    }

    public void removeChunks(Set<Chunk> toRemove) {
        for (Chunk chunk : toRemove) {
            ChunkReference ref = new ChunkReference(chunk);
            chunkFreeQueue.add(ref);
            chunks.remove(ref);
        }
    }

    public void preSnapshotRun(Transform transform) {
        if (session.getEngine().get(Client.class) != null) {
            return;
        }

        // We want to free all chunks first
        chunkFreeQueue.forEach(this::freeChunk);
        chunkFreeQueue.clear();

        // We will sync old chunks, but not new ones
        Set<ChunkReference> toSync = new LinkedHashSet<>(activeChunks);

        // Now send new chunks
        int chunksSentThisTick = 0;

        // We always send all priority chunks
        // Send regular chunks while we aren't overloaded and we haven't exceeded our send amount
        Iterator<ChunkReference> i = chunkSendQueue.iterator();
        while (i.hasNext() && (chunksSentThisTick < CHUNKS_PER_TICK && !session.getEngine().getScheduler().isServerOverloaded())) {
            Chunk c = i.next().refresh(LoadOption.NO_LOAD, session.getEngine().getWorldManager());
            if (c != null && attemptSendChunk(c)) {
                chunksSentThisTick++;
                i.remove();
            }
        }

        sendPositionUpdates(transform);
        previousTransform = transform;

        // Update the active chunks
        for (Iterator<ChunkReference> it = toSync.iterator(); it.hasNext();) {
            ChunkReference ref = it.next();
            Chunk chunk = ref.get();
            // If it was unloaded, we have to free it
            // We don't remove it from our chunks though
            if (chunk == null) {
                System.out.println("Active chunk (" + ref.getBase().getChunkX() + " " + ref.getBase().getChunkY() + " " + ref.getBase().getChunkZ() + ") has been unloaded! Freeing from client. (Will try to send next tick");
                freeChunk(ref);
                chunkSendQueue.add(ref);
                continue;
            }
            //chunk.sync(this);
        }
    }

    private void freeChunk(ChunkReference ref) {
        //callProtocolEvent(new ChunkFreeEvent(ref.getBase()), player);
    }

    private boolean attemptSendChunk(Chunk c) {
        if (!canSendChunk(c)) {
            return false;
        }

        //callProtocolEvent(new ChunkSendEvent(c), player);
        // TODO: use ChunkSnapshot
        session.send(new ChunkDataMessage(c.getChunkX(), c.getChunkY(), c.getChunkZ(), ((FlowChunk) c).getBlockStore().getFullArray()));
        ChunkReference ref = new ChunkReference(c);
        activeChunks.add(ref);
        return true;
    }

    protected boolean canSendChunk(Chunk c) {
        return true;
    }

    private void sendPositionUpdates(Transform transform) {
        if (!transform.equals(previousTransform)) {
            session.send(new UpdateEntityMessage(-1, transform, UpdateEntityMessage.UpdateAction.TRANSFORM, NullRepositionManager.INSTANCE));
            //callProtocolEvent(new EntityUpdateEvent(player, live, EntityUpdateEvent.UpdateAction.TRANSFORM, getRepositionManager()), player);
        }
    }
}
