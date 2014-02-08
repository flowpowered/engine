/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package com.flowpowered.api.player;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.api.Client;
import com.flowpowered.api.Flow;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.reference.ChunkReference;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.player.reposition.NullRepositionManager;
import com.flowpowered.api.player.reposition.RepositionManager;
import com.flowpowered.api.util.SyncedStringMap;
import com.flowpowered.commons.concurrent.set.TSyncIntHashSet;
import com.flowpowered.commons.store.MemoryStore;
import com.flowpowered.engine.util.OutwardIterator;
import com.flowpowered.events.EventHandler;
import com.flowpowered.events.Listener;
import com.flowpowered.events.Order;
import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.networking.session.BasicSession;
import com.flowpowered.networking.session.Session;

/**
 * The networking behind {@link org.spout.api.entity.Player}s. This component holds the {@link Session} which is the connection the Player has to the server.
 */
public class PlayerNetwork implements Listener {
	private static final SyncedStringMap protocolMap = SyncedStringMap.create(null, new MemoryStore<Integer>(), 0, 256, "componentProtocols");
	protected static final int CHUNKS_PER_TICK = 20;
	private final AtomicReference<BasicSession> session = new AtomicReference<>(null);
	protected final TSyncIntHashSet synchronizedEntities = new TSyncIntHashSet();
	private Point lastChunkCheck = Point.INVALID;

	private final Set<ChunkReference> chunkSendQueuePriority = new LinkedHashSet<>();
	private final Set<ChunkReference> chunkSendQueueRegular = new LinkedHashSet<>();
	private final Set<ChunkReference> chunkFreeQueue = new LinkedHashSet<>();
	/**
	 * Chunks that have been sent to the client
	 */
	private final Set<ChunkReference> activeChunks = new LinkedHashSet<>();
	/**
	 * Includes chunks that need to be sent.
	 */
	private final Set<ChunkReference> futureChunksToSend = new LinkedHashSet<>();

	protected volatile boolean worldChanged = false;
    protected volatile Transform previousTransform = null;
	private boolean sync = false;
	protected int tickCounter = 0;
	private int chunksSent = 0;
	private final AtomicReference<RepositionManager> rm = new AtomicReference<>(NullRepositionManager.getInstance());

    private final Player player;

    public PlayerNetwork(Player player) {
        this.player = player;
    }

	/**
	 * Returns the {@link Session} representing the connection to the server.
	 *
	 * @return The session
	 */
	public final BasicSession getSession() {
		return session.get();
	}

	/**
	 * Sets the session this Player has to the server.
	 *
	 * @param session The session to the server
	 */
	public final void setSession(BasicSession session) {
		if (!this.session.compareAndSet(null, session)) {
			throw new IllegalStateException("Once set, the session may not be re-set until a new connection is made");
		}
	}

	/**
	 * Gets the {@link InetAddress} of the session
	 *
	 * @return The address of the session
	 */
	public final InetAddress getAddress() {
		return getSession().getAddress().getAddress();
	}

	/**
	 * Registers the protocol name and gets the id assigned.
	 *
	 * @param protocolName The name of the protocol class to get an id for
	 * @return The id for the specified protocol class
	 */
	public static int getProtocolId(String protocolName) {
		return protocolMap.register(protocolName);
	}

//	/**
//	 * Instructs the client to update the entities state and position<br><br>
//	 *
//	 * @param event {@link EntitySyncEvent}
//	 */
//	@EventHandler(order = Order.EARLIEST)
//	public final void syncEntityEarliest(EntityUpdateEvent event) {
//		if (Spout.getPlatform() != Platform.SERVER) {
//			return;
//		}
//		switch (event.getAction()) {
//			case ADD:
//				synchronizedEntities.add(event.getEntityId());
//				break;
//			case REMOVE:
//				synchronizedEntities.remove(event.getEntityId());
//				break;
//		}
//	}

	public boolean hasSpawned(Entity e) {
		return synchronizedEntities.contains(e.getId());
	}

	public void forceRespawn() {
		worldChanged = true;
	}

	public void forceSync() {
		sync = true;
	}

	/**
	 * Gets the reposition manager that converts local coordinates into remote coordinates
	 */
	public RepositionManager getRepositionManager() {
		return rm.get();
	}

	public void setRepositionManager(RepositionManager rm) {
		if (rm == null) {
			this.rm.set(NullRepositionManager.getInstance());
		} else {
			this.rm.set(rm);
		}
	}



	/**
	 * Checks for chunk updates that might have from movement.
	 */
	private void checkChunkUpdates(Point currentPosition) {
		// Recalculating these
		if (!chunkFreeQueue.isEmpty()) {
			throw new IllegalStateException("chunkFreeQueue is not empty!");
		}
		chunkSendQueuePriority.clear();
		chunkSendQueueRegular.clear();
		futureChunksToSend.clear();

		final World world = currentPosition.getWorld();
		final int bx = currentPosition.getBlockX();
		final int by = currentPosition.getBlockX();
		final int bz = currentPosition.getBlockX();
		final int cx = bx >> Chunk.BLOCKS.BITS;
		final int cy = by >> Chunk.BLOCKS.BITS;
		final int cz = bz >> Chunk.BLOCKS.BITS;
		Point playerChunkBase = Chunk.pointToBase(currentPosition);
		for (ChunkReference ref : activeChunks) {
			Point p = ref.getBase();
			if (!isInViewVolume(p, playerChunkBase, getSyncDistance())) {
				chunkFreeQueue.add(ref);
			}
		}

		// TODO: could we use getSyncIterator?
		Iterator<Vector3i> itr = getViewableVolume(cx, cy, cz, getSyncDistance());
		while (itr.hasNext()) {
			Vector3i v = itr.next();
			Point base = new Point(world, v.getX() << Chunk.BLOCKS.BITS, v.getY() << Chunk.BLOCKS.BITS, v.getZ() << Chunk.BLOCKS.BITS);
			ChunkReference ref = new ChunkReference(base);
			if (activeChunks.contains(ref)) {
				continue;
			}
			boolean inTargetArea = getMaxDistance(playerChunkBase, base) <= (getSyncDistance() / 2); // TODO: do we need to move blockMinViewDistance?
			boolean needGen = !inTargetArea;
			// If it's in the target area, we first check if we can just load it. If so, do that
			// If not, queue it for LOAD_GEN, but don't wait
			// If it's not in the target area, don't even wait for load
			if (inTargetArea && ref.refresh(LoadOption.LOAD_ONLY) == null) {
					needGen = true;
			}
			if (needGen) {
				ref.refresh(LoadOption.LOAD_GEN_NOWAIT);
			}

			futureChunksToSend.add(ref);
		}
	
	}

	private void updateSendLists(Point currentPosition) {
		Point playerChunkBase = Chunk.pointToBase(currentPosition);
		for (Iterator<ChunkReference> it = futureChunksToSend.iterator(); it.hasNext();) {
			ChunkReference ref = it.next();
			if (ref.refresh(LoadOption.NO_LOAD) == null) continue;
			it.remove();
			boolean inTargetArea = getMaxDistance(playerChunkBase, ref.getBase()) <= (getSyncDistance() / 2);
			if (inTargetArea) {
				chunkSendQueuePriority.add(ref);
			} else {
				chunkSendQueueRegular.add(ref);
			}
		}
	}

	/**
	 * Called when the owner is set to be synchronized to other NetworkComponents.
	 *
	 * TODO: Common logic between Spout and a plugin needing to implement this component? TODO: Add sequence checks to the PhysicsComponent to prevent updates to live?
	 *
	 * @param live A copy of the owner's live transform state
	 */
	public void finalizeRun() {
		if (Flow.getPlatform().isServer()) {
			return;
		}
		tickCounter++;
		//TODO: update chunk lists?
		final int prevSyncDistance = getSyncDistance();
		final int currentSyncDistance = getSyncDistance();
		final Point currentPosition = player.getTransformProvider().getTransform().getPosition();
        if (!currentPosition.getWorld().equals(previousTransform == null ? null : previousTransform.getPosition())) {
            worldChanged = true;
        }
		if (prevSyncDistance != currentSyncDistance || worldChanged || (!currentPosition.equals(lastChunkCheck) && getManhattanDistance(currentPosition, lastChunkCheck) > (Chunk.BLOCKS.SIZE / 2))) {
			checkChunkUpdates(currentPosition);
			lastChunkCheck = currentPosition;
		}

		updateSendLists(currentPosition);
	}

	/**
	 * Called just before a snapshot is taken of the owner.
	 *
	 * TODO: Add sequence checks to the PhysicsComponent to prevent updates to live?
	 *
	 * @param live A copy of the owner's live transform state
	 */
	public void preSnapshotRun() {
		if (Flow.getPlatform().isClient()) {
			return;
		}

        Point ep = player.getTransformProvider().getTransform().getPosition();
		if (worldChanged) {
			resetChunks();
			//callProtocolEvent(new WorldChangeProtocolEvent(ep.getWorld()), player);
			worldChanged = false;
			sync = true;
		} else {
			// We will update old chunks, but not new ones
			Set<ChunkReference> toSync = new LinkedHashSet<>(activeChunks);

			// Now send new chunks
			chunksSent = 0;

			// Send priority chunks first
			sendChunks(chunkSendQueuePriority.iterator(), true);

			// If we didn't send all the priority chunks, don't send position or regular chunks yet
			if (chunkSendQueuePriority.isEmpty()) {
				// Send position
				sendPositionUpdates();

				// Then regular chunks
				sendChunks(chunkSendQueueRegular.iterator(), false);
			}

			Set<ChunkReference> freeChunks = freeChunks();
			if (!freeChunks.isEmpty() && !toSync.removeAll(freeChunks)) {
				throw new IllegalStateException("There were freed chunks, but they were not removed.");
			}

			for (Iterator<ChunkReference> it = toSync.iterator(); it.hasNext();) {
				ChunkReference ref = it.next();
				Chunk chunk = ref.get();
				if (chunk == null) {
					System.out.println("Active chunk (" + ref.getBase().getChunkX() + " " + ref.getBase().getChunkY() + " " + ref.getBase().getChunkZ() + ") has been unloaded! Adding toChunkFreeQueue");
					chunkFreeQueue.add(ref);
					continue;
				}
				//chunk.sync(this);
			}

			// We run another free to be sure all chunks that became free are now freed, this tick
			freeChunks();

            previousTransform = player.getTransformProvider().getTransform();
		}
	}

	private Set<ChunkReference> freeChunks() {
		HashSet<ChunkReference> freed = new HashSet<>();
		for (ChunkReference ref : chunkFreeQueue) {
			//callProtocolEvent(new ChunkFreeEvent(ref.getBase()), player);
			freed.add(ref);
			activeChunks.remove(ref);
		}
		chunkFreeQueue.clear();
		return freed;
	}

	private void sendChunks(Iterator<ChunkReference> i, boolean priority) {
		while (i.hasNext() && (priority || (chunksSent < CHUNKS_PER_TICK && !Flow.getEngine().getScheduler().isServerOverloaded()))) {
			Chunk c = i.next().get();
			if (c == null || attemptSendChunk(c)) {
				i.remove();
			}
		}
	}

	private void sendPositionUpdates() {
		if (player.getTransformProvider().getTransform().equals(previousTransform) && sync) {
			//callProtocolEvent(new EntityUpdateEvent(player, live, EntityUpdateEvent.UpdateAction.TRANSFORM, getRepositionManager()), player);
			sync = false;
		}
	}

	/**
	 * Resets all chunk stores for the client.  This method is only called during the pre-snapshot part of the tick.
	 */
	protected void resetChunks() {
		futureChunksToSend.clear();
		chunkSendQueuePriority.clear();
		chunkSendQueueRegular.clear();
		chunkFreeQueue.clear();
		activeChunks.clear();
		lastChunkCheck = Point.INVALID;
		synchronizedEntities.clear();
	}

	protected boolean canSendChunk(Chunk c) {
		return true;
	}

	private boolean attemptSendChunk(Chunk c) {
		if (!canSendChunk(c)) {
			return false;
		}

		//callProtocolEvent(new ChunkSendEvent(c), player);
		ChunkReference ref = new ChunkReference(c);
		activeChunks.add(ref);
		chunksSent++;
		return true;
	}

	/**
	 * Returns a copy of all currently active sent chunks to this player
	 *
	 * @return active chunks
	 */
	public Set<Chunk> getActiveChunks() {
		HashSet<Chunk> chunks = new HashSet<>();
		for (ChunkReference p : activeChunks) {
			Chunk get = p.get();
			if (get != null) {
				chunks.add(get);
			}
		}
		return chunks;
	}

	public void reset() {
		session.set(null);
	}


	/**
	 * Gets the viewable volume centered on the given chunk coordinates and the given view distance
	 */
	public Iterator<Vector3i> getViewableVolume(int cx, int cy, int cz, int viewDistance) {
		return new OutwardIterator(cx, cy, cz, viewDistance);
	}

	/**
	 * Test if a given chunk base is in the view volume for a given player chunk base point
	 *
	 * @return true if in the view volume
	 */
	public boolean isInViewVolume(Point playerChunkBase, Point testChunkBase, int viewDistance) {
		return getManhattanDistance(testChunkBase, playerChunkBase) <= (viewDistance << Chunk.BLOCKS.BITS);
	}

    public int getSyncDistance() {
        //return player.getObserver().getSyncDistance()
        return 10;
    }

	/**
	 * Gets the Manhattan distance between two points.
	 *
	 * This will return Double.MAX_VALUE if the other Point is null, either world is null, or the two points are in different worlds.
	 *
	 * Otherwise, it returns the Manhattan distance.
	 */
	public static double getManhattanDistance(Point one, Point other) {
		if (other == null || one.getWorld() == null || other.getWorld() == null || !one.getWorld().equals(other.getWorld())) {
			return Double.MAX_VALUE;
		}
		return Math.abs(one.getVector().getX() - other.getVector().getX()) + Math.abs(one.getVector().getY() - other.getVector().getY()) + Math.abs(one.getVector().getZ() - other.getVector().getZ());
	}

	/**
	 * Gets the largest distance between two points, when projected onto one of the axes.
	 *
	 * This will return Double.MAX_VALUE if the other Point is null, either world is null, or the two points are in different worlds.
	 *
	 * Otherwise, it returns the max distance.
	 */
	public static double getMaxDistance(Point one, Point other) {
		if (other == null || one.getWorld() == null || other.getWorld() == null || !one.getWorld().equals(other.getWorld())) {
			return Double.MAX_VALUE;
		}
		return Math.max(Math.abs(one.getVector().getX() - other.getVector().getX()),
						Math.max(Math.abs(one.getVector().getY() - other.getVector().getY()),
						Math.abs(one.getVector().getZ() - other.getVector().getZ())));
	}
}
