package com.flowpowered.engine.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Type-safe generic registry object
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 * @see <a href="http://musingsofaprogrammingaddict.blogspot.de/2009/11/generic-and-still-type-safe-dto-part-2.html">A generic and yet type-safe DTO - part 2</a>
 */
public class GenericRegistryObject<G extends AttributeGroup> {

	private Class<G> attributeGroup;
	private Map<Attribute<G, ?>, Object> attributes = new LinkedHashMap<Attribute<G, ?>, Object>();
	private String name;

	GenericRegistryObject(Class<G> attributeGroup) {
		this.attributeGroup = attributeGroup;
	}

	public static <B extends AttributeGroup> GenericRegistryObject<B> getInstance(
			Class<B> clazz) {
		return new GenericRegistryObject<B>(clazz);
	}

	public <T> GenericRegistryObject<G> set(Attribute<G, T> identifier, T value) {
		attributes.put(identifier, value);
		return this;
	}

	public <T> T get(Attribute<G, T> identifier) {
		@SuppressWarnings("unchecked")
		T theValue = (T) attributes.get(identifier);
		return theValue;
	}

	public <T> T remove(Attribute<G, T> identifier) {
		@SuppressWarnings("unchecked")
		T theValue = (T) attributes.remove(identifier);
		return theValue;
	}

	public void clear() {
		attributes.clear();
	}

	public int size() {
		return attributes.size();
	}

	public Set<Attribute<G, ?>> getAttributes() {
		return attributes.keySet();
	}

	public boolean contains(Attribute<G, ?> identifier) {
		return attributes.containsKey(identifier);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return attributeGroup.getSimpleName() + " [" + attributes + "]";
	}
}
