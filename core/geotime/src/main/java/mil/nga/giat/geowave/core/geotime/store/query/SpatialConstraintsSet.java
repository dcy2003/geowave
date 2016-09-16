package mil.nga.giat.geowave.core.geotime.store.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class SpatialConstraintsSet {
	final Map<String, SpatialConstraints> constraintsSet = new HashMap<String, SpatialConstraints>();

	public SpatialConstraintsSet() {}

	public SpatialConstraints getConstraintsFor(
			final String fieldName ) {
		if (constraintsSet.containsKey(fieldName)) {
			return constraintsSet.get(fieldName);
		}
		else {
			final SpatialConstraints constraints = new SpatialConstraints(
					fieldName);
			constraintsSet.put(
					fieldName,
					constraints);
			return constraints;
		}
	}

	public void removeConstraints(
			final String... names ) {
		for (String name : names)
			constraintsSet.remove(name);
	}

	public void removeAllConstraintsExcept(
			final String... names ) {
		final Map<String, SpatialConstraints> newConstraintsSet = new HashMap<String, SpatialConstraints>();
		for (final String name : names) {
			final SpatialConstraints constraints = constraintsSet.get(name);
			if (constraints != null) {
				newConstraintsSet.put(
						name,
						constraints);
			}
		}
		constraintsSet.clear();
		constraintsSet.putAll(newConstraintsSet);
	}

	public boolean hasConstraintsFor(
			final String propertyName ) {
		return (propertyName != null) && constraintsSet.containsKey(propertyName);
	}

	public Set<Entry<String, SpatialConstraints>> getSet() {
		return constraintsSet.entrySet();
	}

	public boolean isInfinite() {

		if (constraintsSet.isEmpty()) {
			return true;
		}
		boolean isInfinite = true;
		for (final Entry<String, SpatialConstraints> entry : getSet()) {
			isInfinite &= entry.getValue().isInfinite();
		}
		return isInfinite;
	}
	
	public void intersectWith(
			final SpatialConstraintsSet sideR ) {

		for (final Map.Entry<String, SpatialConstraints> entry : sideR.getSet()) {
			getConstraintsFor(
					entry.getKey()).replaceWithIntersections(
					entry.getValue());
		}
	}

	public void mergeWith(
			final SpatialConstraintsSet sideR ) {
		for (final Map.Entry<String, SpatialConstraints> entry : sideR.getSet()) {
			getConstraintsFor(
					entry.getKey()).replaceWithMerged(
					entry.getValue());
		}
	}
}
