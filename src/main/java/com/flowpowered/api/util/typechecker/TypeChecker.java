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
package com.flowpowered.api.util.typechecker;

import org.spout.nbt.Tag;

public class TypeChecker<T> extends com.flowpowered.commons.typechecker.TypeChecker<T> {

	public TypeChecker(Class<T> clazz) {
		super(clazz);
	}

	/**
	 * Checks and casts an object contained in a tag to the specified type.
	 *
	 * @param tag The Tag containing the object to be checked
	 *
	 * @return The object contained in the tag, cast to the specified class
	 *
	 * @throws ClassCastException if casting fails
	 */
	public final T checkTag(Tag<?> tag) {
		return check(tag.getValue());
	}

	/**
	 * Checks and casts an object to the specified type. If casting fails, a default value is returned.
	 *
	 * @param tag          The Tag containing the object to be checked
	 * @param defaultValue The default value to be returned if casting fails
	 *
	 * @return The object contained in the tag, cast to the specified class, or the default value, if casting fails
	 */
	public final T checkTag(Tag<?> tag, T defaultValue) {
		if (tag == null) {
			return defaultValue;
		}

		try {
			return check(tag.getValue());
		} catch (ClassCastException e) {
			return defaultValue;
		}
	}
}
