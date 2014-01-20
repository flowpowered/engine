package com.flowpowered.engine.render.model;

import java.util.concurrent.Future;

import com.flowpowered.math.vector.Vector3i;

import com.flowpowered.engine.render.SpoutRenderer;
import org.spout.renderer.api.data.VertexData;
import org.spout.renderer.api.gl.VertexArray;
import org.spout.renderer.api.model.Model;

/**
 * In the case that meshing is occurring and that the chunk is not renderable, a previous model can be rendered instead. To use this feature, set the previous model using {@link
 * org.spoutcraft.client.nterface.mesh.ParallelChunkMesher.ChunkModel#setPrevious(org.spoutcraft.client.nterface.mesh.ParallelChunkMesher.ChunkModel)}. This previous model will be used until the mesh
 * becomes available. At this point, the previous model will be destroyed, and the new one rendered. When a model isn't needed anymore, you must call {@link
 * org.spoutcraft.client.nterface.mesh.ParallelChunkMesher.ChunkModel#destroy()} to dispose of it completely. This will also cancel the meshing if it's in progress, and destroy the previous model. The
 * chunk can also be automatically culled by passing an optional {@link org.spoutcraft.client.nterface.Interface} to the {@link org.spoutcraft.client.nterface.mesh.ParallelChunkMesher} constructor.
 */
public class ChunkModel extends Model {
    private final SpoutRenderer renderer;
    private volatile Future<VertexData> mesh;
    private volatile boolean complete = false;
    private ChunkModel previous;

    public ChunkModel(SpoutRenderer renderer, Vector3i position, Future<VertexData> mesh) {
        this.renderer = renderer;
        this.mesh = mesh;
        // TODO: replace magic number
        setPosition(position.mul(16).toFloat());
    }

    @Override
    public void render() {
        // If we have not received the mesh and it's done
        if (!complete && mesh.isDone()) {
            // Get the mesh
            final VertexData vertexData;
            try {
                vertexData = mesh.get();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            // If the chunk mesher returned a mesh. It may not return one if the chunk has no mesh (completely invisible)
            if (vertexData != null) {
                // Create the vertex array from the mesh
                final VertexArray vertexArray = renderer.getGLFactory().createVertexArray();
                vertexArray.setData(vertexData);
                vertexArray.create();
                // Set it for rendering
                setVertexArray(vertexArray);
            }
            // Destroy and discard the previous model (if any), as it is now obsolete
            if (previous != null) {
                previous.destroy();
                previous = null;
            }
            // Set the model as complete
            complete = true;
            mesh = null;
        }
        if (!isVisible()) {
            return;
        }
        // If we have a vertex array, we can render
        if (complete) {
            // Only render if the model has a vertex array and we're visible
            if (getVertexArray() != null && isVisible()) {
                super.render();
            }
        } else if (previous != null && isVisible()) {
            // Else, fall back on the previous model if we have one and we're visible
            previous.render();
        }
    }

    private boolean isVisible() {
        // It's hard to look right
        // at the world baby
        // But here's my frustum
        // so cull me maybe?
        return true;
        // TODO frustrum
    }

    /**
     * Sets the previous model to renderer until the updated one is ready.
     *
     * @param previous The previous model
     */
    public void setPrevious(ChunkModel previous) {
        this.previous = previous;
    }

    /**
     * Destroys the models, cancelling the meshing task if in progress, and the previous model (if any).
     */
    public void destroy() {
        // If we have a vertex array, destroy it
        if (complete) {
            if (getVertexArray() != null) {
                getVertexArray().destroy();
            }
            complete = false;
        } else {
            // Else, the mesh is still in progress, cancel that
            if (mesh != null) {
                mesh.cancel(false);
                mesh = null;
            }
            // Also destroy and discard the previous model if we have one
            if (previous != null) {
                previous.destroy();
                previous = null;
            }
        }
    }

    public void remove() {
        renderer.removeModel(this);
        destroy();
    }
}
