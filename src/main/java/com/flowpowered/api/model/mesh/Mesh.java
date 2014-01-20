/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spoutcraft <http://spoutcraft.org/>
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
package com.flowpowered.api.model.mesh;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import org.spout.renderer.api.data.VertexAttribute;
import org.spout.renderer.api.data.VertexAttribute.DataType;
import org.spout.renderer.api.data.VertexData;
import org.spout.renderer.api.util.CausticUtil;

/**
 * Represents a standard mesh, with various attributes (positions, normals, texture coordinates and/or tangents). This mesh can be converted into {@link org.spout.renderer.data.VertexData for
 * rendering}.
 *
 * @see org.spoutcraft.client.nterface.mesh.Mesh.MeshAttribute
 */
public class Mesh {
    private final Map<MeshAttribute, TFloatList> attributes = new EnumMap<>(MeshAttribute.class);
    private final TIntList indices = new TIntArrayList();

    /**
     * Constructs a new mesh with the desired attributes.
     *
     * @param attributes The attributes
     */
    public Mesh(MeshAttribute... attributes) {
        for (MeshAttribute attribute : attributes) {
            this.attributes.put(attribute, new TFloatArrayList());
        }
    }

    /**
     * Returns true if the mesh has the attribute, false if not.
     *
     * @param attribute Whether or not the mesh has the attribute
     * @return The attribute to check for
     */
    public boolean hasAttribute(MeshAttribute attribute) {
        return attributes.containsKey(attribute);
    }

    /**
     * Adds an attribute to the mesh, if not already present.
     *
     * @param attribute The attribute to add
     */
    public void addAttribute(MeshAttribute attribute) {
        if (!hasAttribute(attribute)) {
            attributes.put(attribute, new TFloatArrayList());
        }
    }

    /**
     * Returns the float list associated to the attribute in which to store the data. The components for each individual attribute point should be stored in their natural order inside the list, and
     * each point after the other. Actual order of the points for rendering is decided by the indices list.
     *
     * @param attribute The attribute to get the float list for
     * @return The float list for the attribute
     */
    public TFloatList getAttribute(MeshAttribute attribute) {
        return attributes.get(attribute);
    }

    /**
     * Removes the attributes from the mesh, deleting its data.
     *
     * @param attribute The attribute to remove
     */
    public void removeAttribute(MeshAttribute attribute) {
        attributes.remove(attribute);
    }

    /**
     * Returns the index list for the mesh, in which to store the indices that declares the triangle faces by winding order.
     *
     * @return The index list
     */
    public TIntList getIndices() {
        return indices;
    }

    /**
     * Returns true if all the attribute data lists and the indices list are empty.
     *
     * @return Whether or not this mesh is empty (no data for the attributes or indices)
     */
    public boolean isEmpty() {
        for (TFloatList list : attributes.values()) {
            if (!list.isEmpty()) {
                return false;
            }
        }
        return indices.isEmpty();
    }

    /**
     * Builds the mesh into a {@link org.spout.renderer.data.VertexData} to be ready for rendering. If an attribute has no data, but can be automatically generated (see {@link
     * org.spoutcraft.client.nterface.mesh.Mesh.MeshAttribute#generateDataIfMissing()}, it will be generated for the build. The generated data will be stored in the attribute float list.
     *
     * @return The vertex data for the built mesh
     */
    public VertexData build() {
        final VertexData vertexData = new VertexData();
        int i = 0;
        for (Entry<MeshAttribute, TFloatList> entry : attributes.entrySet()) {
            MeshAttribute attribute = entry.getKey();
            final VertexAttribute vertexAttribute = new VertexAttribute(attribute.getName(), DataType.FLOAT, attribute.getComponentCount());
            final TFloatList data = entry.getValue();
            if (data.isEmpty() && attribute.generateDataIfMissing()) {
                switch (attribute) {
                    case NORMALS:
                        CausticUtil.generateNormals(attributes.get(MeshAttribute.POSITIONS), indices, data);
                        break;
                    case TANGENTS:
                        CausticUtil.generateTangents(attributes.get(MeshAttribute.POSITIONS), attributes.get(MeshAttribute.NORMALS), attributes.get(MeshAttribute.TEXTURE_COORDS), indices, data);
                }
            }
            vertexAttribute.setData(data);
            vertexData.addAttribute(i++, vertexAttribute);
        }
        vertexData.getIndices().addAll(indices);
        return vertexData;
    }

    /**
     * An enum of the various mesh attributes.
     */
    public static enum MeshAttribute {
        // Enum ordering is important here, don't change
        /**
         * The positions attribute, has 3 components and cannot be automatically generated.
         */
        POSITIONS("positions", 3, false),
        /**
         * The normals attribute, has 3 components and can be automatically generated if the position data exists.
         */
        NORMALS("normals", 3, true),
        /**
         * The texture coordinates attribute, has 2 components and cannot be automatically generated.
         */
        TEXTURE_COORDS("textureCoords", 2, false),
        /**
         * The tangents attribute, has 4 components and can be automatically generated if the positions, normals and texture coordinates exist.
         */
        TANGENTS("tangents", 4, true);
        private final String name;
        private final int componentCount;
        private final boolean generateIfDataMissing;

        private MeshAttribute(String name, int componentCount, boolean generateIfDataMissing) {
            this.name = name;
            this.componentCount = componentCount;
            this.generateIfDataMissing = generateIfDataMissing;
        }

        /**
         * Returns the name of the attribute.
         *
         * @return The attribute name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the component count of the attribute.
         *
         * @return The component count
         */
        public int getComponentCount() {
            return componentCount;
        }

        /**
         * Returns true if the attribute data can be automatically generated when the required attributes are present.
         *
         * @return Whether or not the attribute data can be automatically generated
         */
        public boolean generateDataIfMissing() {
            return generateIfDataMissing;
        }
    }
}
