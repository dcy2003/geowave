package mil.nga.giat.geowave.core.geotime.store.query;

import com.vividsolutions.jts.geom.Geometry;

import mil.nga.giat.geowave.core.geotime.GeometryUtils;

public class SpatialConstraints {
	private Geometry geometry = null;
	private String name = null;

	public SpatialConstraints() {
		this.geometry = infinity();
	}

	public SpatialConstraints(
			String name ) {
		this.geometry = infinity();
		this.name = name;
	}
	
	public SpatialConstraints(
			Geometry geometry ) {
		this.geometry = geometry;
	}

	public SpatialConstraints(
			Geometry geometry,
			String name ) {
		if (geometry == null) {
			throw new RuntimeException("Invalid geometry (null)");
		}
		this.geometry = geometry;
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public Geometry getGeometry() {
		return geometry;
	}
	
	public boolean isNamed() {
		return name != null;
	}
	
	public boolean isInfinite() {
		double mixedArea = geometry.getArea();
		if (Double.isInfinite(mixedArea) || Double.isNaN(mixedArea)) {
			return true;
		}
		return false;
	}

	public static Geometry infinity() {
		return GeometryUtils.infinity();
	}
	
	public void replaceWithIntersections(
			final SpatialConstraints constraints) {
		
		Geometry mixed = this.geometry;
		Geometry geom = constraints.geometry;
		
		if (isInfinite()) {
			mixed = geom;
		}
		else if (!constraints.isInfinite()) {
			mixed = mixed.intersection(geom);
		}
	
		this.geometry = mixed;
	}

	public void replaceWithMerged(
			final SpatialConstraints constraints ) {
		
		Geometry mixed = this.geometry;
		Geometry geom = constraints.geometry;
		
		if (isInfinite()) {
			mixed = geom;
		}
		else if (!constraints.isInfinite()) {
			mixed = mixed.union(geom);
		}
		
		this.geometry = mixed;
	}

	@Override
	public String toString() {
		return "SpatialConstraints [geometry=" + geometry.toString() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((geometry == null) ? 0 : geometry.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			final Object obj ) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SpatialConstraints other = (SpatialConstraints) obj;
		if (geometry == null) {
			if (other.geometry != null) {
				return false;
			}
		}
		else if (!geometry.equals(other.geometry)) {
			return false;
		}
		return true;
	}
}
