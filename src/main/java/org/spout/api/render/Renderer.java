package org.spout.api.render;

import org.spout.math.vector.Vector2f;

public interface Renderer {

    // TODO: getGLVersion

	/**
	 * Returns the resolution of the window, in pixels.
	 *
	 * @return the resolution of the window.
	 */
	Vector2f getResolution();

	/**
	 * Returns the aspect ratio of the client, in pixels. <p> Ratio = (screen width / screen height)
	 *
	 * @return The ratio as a float
	 */
	float getAspectRatio();
}
