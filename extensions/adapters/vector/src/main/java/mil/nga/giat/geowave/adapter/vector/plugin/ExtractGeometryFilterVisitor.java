package mil.nga.giat.geowave.adapter.vector.plugin;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.geotools.filter.visitor.NullFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.And;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import mil.nga.giat.geowave.core.geotime.store.query.SpatialConstraints;
import mil.nga.giat.geowave.core.geotime.store.query.SpatialConstraintsSet;

/**
 * This class can be used to get Geometry from an OpenGIS filter object. GeoWave
 * then uses this geometry to perform a spatial intersection query.
 * 
 */
public class ExtractGeometryFilterVisitor extends
		NullFilterVisitor
{
	public static final NullFilterVisitor GEOMETRY_VISITOR = new ExtractGeometryFilterVisitor(
			GeoWaveGTDataStore.DEFAULT_CRS);

	private static Logger LOGGER = Logger.getLogger(ExtractGeometryFilterVisitor.class);

	private final CoordinateReferenceSystem crs;

	/**
	 * This FilterVisitor is stateless - use
	 * ExtractGeometryFilterVisitor.BOUNDS_VISITOR. You may also subclass in
	 * order to reuse this functionality in your own FilterVisitor
	 * implementation.
	 */
	public ExtractGeometryFilterVisitor(
			final CoordinateReferenceSystem crs ) {
		this.crs = crs;
	}
	
	public static SpatialConstraintsSet getConstraints(final Filter filter,
			CoordinateReferenceSystem crs ) {
		final Object output = filter.accept(
				new ExtractGeometryFilterVisitor(
						crs),
				null);

		if (output instanceof SpatialConstraintsSet) {
			return (SpatialConstraintsSet) output;
		}
		else if (output instanceof SpatialConstraints) {
			final SpatialConstraints paramConstraint = (SpatialConstraints) output;
			final SpatialConstraintsSet constraintSet = new SpatialConstraintsSet();
			constraintSet.getConstraintsFor(
					paramConstraint.getName()).replaceWithMerged(
					paramConstraint);
			return constraintSet;
		}
		return new SpatialConstraintsSet();
	}	
	

	/**
	 * Produce an ReferencedEnvelope from the provided data parameter.
	 * 
	 * @param data
	 * @return ReferencedEnvelope
	 */
	private SpatialConstraints bbox(
			final Object data ) {
		try {
			if (data == null) {
				return null;
			}
			else if (data instanceof ReferencedEnvelope) {
				return new SpatialConstraints(new GeometryFactory().toGeometry(((ReferencedEnvelope) data).transform(
						crs,
						true)));
			}
			else if (data instanceof Envelope) {
				return new SpatialConstraints(
						new GeometryFactory().toGeometry((Envelope) data));
			}
			else if (data instanceof CoordinateReferenceSystem) {
				return new SpatialConstraints(
						new GeometryFactory().toGeometry(new ReferencedEnvelope(
								(CoordinateReferenceSystem) data).transform(
								crs,
								true)));
			}
			else if (data instanceof SpatialConstraints) {
				return (SpatialConstraints)data;
			}
		}
		catch (TransformException | FactoryException e) {
			LOGGER.warn(
					"Unable to transform geometry",
					e);
			return null;
		}
		throw new ClassCastException(
				"Could not cast data to ReferencedEnvelope");
	}

	@Override
	public Object visit(
			final ExcludeFilter filter,
			final Object data ) {
		return null;
	}

	@Override
	public Object visit(
			final IncludeFilter filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final BBOX filter,
			final Object data ) {

		// we have to take the reference geometry bbox and
		// expand it by the distance.
		// We ignore the unit of measure for the moment
		Literal geometry = null;
		PropertyName propertyName = null;
		if ((filter.getExpression1() instanceof PropertyName) && (filter.getExpression2() instanceof Literal)) {
			geometry = (Literal) filter.getExpression2();
			propertyName = (PropertyName) filter.getExpression1();
		}
		if ((filter.getExpression2() instanceof PropertyName) && (filter.getExpression1() instanceof Literal)) {
			geometry = (Literal) filter.getExpression1();
			propertyName = (PropertyName) filter.getExpression2();
		}

		// we cannot desume a bbox from this filter
		if (geometry == null) {
			return new SpatialConstraints();
		}

		Geometry geom = geometry.evaluate(
				null,
				Geometry.class);
		if (geom == null) {
			return new SpatialConstraints();
		}
		
		return new SpatialConstraints(geom, 
				propertyName.getPropertyName());
	}

	/**
	 * Please note we are only visiting literals involved in spatial operations.
	 * 
	 * @param literal
	 *            , hopefully a Geometry or Envelope
	 * @param data
	 *            Incoming BoundingBox (or Envelope or CRS)
	 * 
	 * @return ReferencedEnvelope updated to reflect literal
	 */
	@Override
	public Object visit(
			final Literal expression,
			final Object data ) {
		final Object value = expression.getValue();
		if (value instanceof Geometry) {
			final Geometry geometry = (Geometry) value;
			return new SpatialConstraints(geometry);
		}
		else {
			LOGGER.debug("LiteralExpression ignored!");
		}
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final And filter,
			final Object data ) {
		SpatialConstraintsSet constraints = new SpatialConstraintsSet();
		for (final Filter f : filter.getChildren()) {
			final Object output = f.accept(
					this,
					data);			
			if (output instanceof SpatialConstraints) {
				SpatialConstraints sc = (SpatialConstraints)output;
				constraints.getConstraintsFor(
						sc.getName()).replaceWithIntersections(
						sc);				
			}
			else if (output instanceof SpatialConstraintsSet) {
				final SpatialConstraintsSet rangeSet = (SpatialConstraintsSet) output;
				constraints.intersectWith(rangeSet);
			}
		}
		return constraints;
	}

	@Override
	public Object visit(
			final Not filter,
			final Object data ) {
		// no matter what we have to return an infinite envelope
		// rationale
		// !(finite envelope) -> an unbounded area -> infinite
		// !(non spatial filter) -> infinite (no spatial concern)
		// !(infinite) -> ... infinite, as the first infinite could be the
		// result
		// of !(finite envelope)

		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final Or filter,
			final Object data ) {
		SpatialConstraintsSet constraints = new SpatialConstraintsSet();
		for (final Filter f : filter.getChildren()) {
			final Object output = f.accept(
					this,
					data);			
			if (output instanceof SpatialConstraints) {
				SpatialConstraints sc = (SpatialConstraints)output;
				constraints.getConstraintsFor(
						sc.getName()).replaceWithMerged(
						sc);				
			}
			else if (output instanceof SpatialConstraintsSet) {
				final SpatialConstraintsSet rangeSet = (SpatialConstraintsSet) output;
				constraints.mergeWith(rangeSet);
			}
		}
		return constraints;
	}

	@Override
	public Object visit(
			final Beyond filter,
			final Object data ) {
		// beyond a certain distance from a finite object, no way to limit it
		return new SpatialConstraints();
	}
	
	private SpatialConstraints mergeResults(Expression left, Expression right, Object data) {
		final SpatialConstraints leftResult = bbox(left.accept(
				this,
				data));
		final SpatialConstraints rightResult = bbox(right.accept(
				this,
				data));
		
		// Just ignore if there are two named constraints.
		if (leftResult.isNamed() && rightResult.isNamed()) {
			return leftResult;
		}

		// Both infinite, don't care!
		if (leftResult.isInfinite() && rightResult.isInfinite()) {
			return new SpatialConstraints();
		}

		SpatialConstraints merged = null;
		if (rightResult.isNamed()) {
			merged = rightResult;
			merged.replaceWithMerged(leftResult);
		}
		else {
			merged = leftResult;
			merged.replaceWithMerged(rightResult);
		}
		return merged;
	}

	@Override
	public Object visit(
			final Contains filter,
			Object data ) {
		return mergeResults(filter.getExpression1(), filter.getExpression2(), data);
	}

	@Override
	public Object visit(
			final Crosses filter,
			Object data ) {
		return mergeResults(filter.getExpression1(), filter.getExpression2(), data);
	}

	@Override
	public Object visit(
			final Disjoint filter,
			final Object data ) {
		// disjoint does not define a rectangle, but a hole in the
		// Cartesian plane, no way to limit it
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final DWithin filter,
			final Object data ) {
		final SpatialConstraints bbox = bbox(data);

		// we have to take the reference geometry bbox and
		// expand it by the distance.
		// We ignore the unit of measure for the moment
		Literal geometry = null;
		PropertyName propertyName = null;
		if ((filter.getExpression1() instanceof PropertyName) && (filter.getExpression2() instanceof Literal)) {
			geometry = (Literal) filter.getExpression2();
			propertyName = (PropertyName) filter.getExpression1();
		}
		if ((filter.getExpression2() instanceof PropertyName) && (filter.getExpression1() instanceof Literal)) {
			geometry = (Literal) filter.getExpression1();
			propertyName = (PropertyName) filter.getExpression2();
		}

		// we cannot desume a bbox from this filter
		if (geometry == null) {
			return new SpatialConstraints();
		}

		Geometry geom = geometry.evaluate(
				null,
				Geometry.class);
		if (geom == null) {
			return new SpatialConstraints();
		}
		Pair<Geometry, Double> geometryAndDegrees;
		try {
			geometryAndDegrees = mil.nga.giat.geowave.adapter.vector.utils.GeometryUtils.buffer(
					crs,
					geom,
					filter.getDistanceUnits(),
					filter.getDistance());
		}
		catch (TransformException e) {
			LOGGER.error(
					"Cannot transform geometry to CRS",
					e);
			geometryAndDegrees = Pair.of(
					geom,
					filter.getDistance());
		}

		if (bbox != null) {
			return new SpatialConstraints(geometryAndDegrees.getLeft().union(
					bbox.getGeometry()), propertyName.getPropertyName());
		}
		else {
			return new SpatialConstraints(geometryAndDegrees.getLeft(), 
					propertyName.getPropertyName());
		}
	}

	@Override
	public Object visit(
			final Equals filter,
			Object data ) {
		return mergeResults(filter.getExpression1(), filter.getExpression2(), data);
	}

	@Override
	public Object visit(
			final Intersects filter,
			Object data ) {
		return mergeResults(filter.getExpression1(), filter.getExpression2(), data);
	}

	@Override
	public Object visit(
			final Overlaps filter,
			Object data ) {
		return mergeResults(filter.getExpression1(), filter.getExpression2(), data);
	}

	@Override
	public Object visit(
			final Touches filter,
			Object data ) {
		return mergeResults(filter.getExpression1(), filter.getExpression2(), data);
	}

	@Override
	public Object visit(
			final Within filter,
			Object data ) {
		return mergeResults(filter.getExpression1(), filter.getExpression2(), data);
	}

	@Override
	public Object visit(
			final Add expression,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final Divide expression,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final Function expression,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final Id filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final Multiply expression,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final NilExpression expression,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsBetween filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsEqualTo filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsGreaterThan filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsGreaterThanOrEqualTo filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsLessThan filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsLessThanOrEqualTo filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsLike filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsNotEqualTo filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyIsNull filter,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visit(
			final PropertyName expression,
			final Object data ) {
		String name = expression.getPropertyName();
		return new SpatialConstraints(name);
	}

	@Override
	public Object visit(
			final Subtract expression,
			final Object data ) {
		return new SpatialConstraints();
	}

	@Override
	public Object visitNullFilter(
			final Object data ) {
		return new SpatialConstraints();
	}

}
