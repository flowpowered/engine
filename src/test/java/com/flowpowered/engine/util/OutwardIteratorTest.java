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
package com.flowpowered.engine.util;

import com.flowpowered.commons.map.TripleIntObjectMap;
import com.flowpowered.commons.map.impl.TTripleInt21ObjectHashMap;
import com.flowpowered.math.vector.Vector3i;
import org.junit.Assert;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OutwardIteratorTest {
	private final int DIST = 10;

	@Test
	public void test() {
        test(new Vector3i(20, 20, 20));
        test(new Vector3i(-100, -100, -100));
	}

    private void test(Vector3i center) {
        final TripleIntObjectMap<Integer> hits = new TTripleInt21ObjectHashMap<>();
		OutwardIterator itr = new OutwardIterator(center.getX(), center.getY(), center.getZ(), DIST);

        Vector3i next = itr.next();
        if (!next.equals(center)) {
            Assert.fail("First not center!");
        }
        add(next, hits);
        next = itr.next();
        if (!next.equals(center.add(0, -1, 0))) {
            Assert.fail("Second not one below! Was: " + next);
        }
        add(next, hits);

		int prev = -1;

		while (itr.hasNext()) {
			next = itr.next();
			int dist = getDistance(center, next);
			assertTrue("Distance readback is incorrect", dist == itr.getDistance());
			assertTrue("Iterator moved inwards", dist >= prev);
			add(next, hits);
			prev = dist;
		}

		check(center, hits);
    }

	private static int getDistance(Vector3i a, Vector3i b) {
		return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
	}

	private void add(Vector3i local, TripleIntObjectMap<Integer> hits) {
        Integer thisHits = hits.get(local.getX(), local.getY(), local.getZ());
        thisHits = (thisHits == null ? 0 : thisHits) + 1;
        hits.put(local.getX(), local.getY(), local.getZ(), thisHits);
		assertTrue("Coordinate hit more than once " + local, thisHits == 1);
	}

	private boolean check(Vector3i center, TripleIntObjectMap<Integer> hits) {
		for (int x = 0; x < DIST + 5; x++) {
			for (int y = 0; y < DIST + 5; y++) {
				for (int z = 0; z < DIST + 5; z++) {
					int distance = getDistance(center, new Vector3i(x, y, z));
					assertTrue("Location missed " + x + " " + y + " " + z, distance > DIST || hits.get(x, y, z) != null);
					assertTrue("Location out of range hit " + x + " " + y + " " + z, distance <= DIST || hits.get(x, y, z) == null);
				}
			}
		}
		return true;
	}
}
