package com.flowpowered.engine.registry;

/**
 * Parametrized identifiers base class
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 * @see <a href="http://musingsofaprogrammingaddict.blogspot.de/2009/11/generic-and-still-type-safe-dto-part-2.html">A generic and yet type-safe DTO - part 2</a>
 */
public class Attribute<G extends AttributeGroup, T> {

	private String name;

	public Attribute(String name) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		this.name = name;
	}

	public static <F extends AttributeGroup, S> Attribute<F, S> getInstance(
			String name) {
		return new Attribute<F, S>(name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Attribute && name.toLowerCase().equals(obj.toString().toLowerCase());
	}
}
