package mil.nga.giat.geowave.adapter.vector.query.cql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map.Entry;

import org.geotools.data.Query;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.Test;
import org.opengis.filter.And;
import org.opengis.filter.Filter;

import mil.nga.giat.geowave.adapter.vector.plugin.ExtractGeometryFilterVisitor;
import mil.nga.giat.geowave.adapter.vector.plugin.ExtractTimeFilterVisitor;
import mil.nga.giat.geowave.core.geotime.store.query.TemporalConstraints;
import mil.nga.giat.geowave.core.index.ByteArrayId;
import mil.nga.giat.geowave.core.store.index.FilterableConstraints;
import mil.nga.giat.geowave.core.store.index.numeric.NumberRangeFilter;
import mil.nga.giat.geowave.core.store.index.numeric.NumericEqualsConstraint;
import mil.nga.giat.geowave.core.store.index.numeric.NumericLessThanConstraint;
import mil.nga.giat.geowave.core.store.index.numeric.NumericQueryConstraint;
import mil.nga.giat.geowave.core.store.index.text.TextExactMatchFilter;
import mil.nga.giat.geowave.core.store.index.text.TextQueryConstraint;

public class PropertyFilterVisitorTest
{
	
	public static void main(String[] args) throws CQLException {
		//Filter filter = CQL.toFilter("DWITHIN(geom, POINT(179.9998 0.79), 13.7, kilometers)");
		Filter filter = CQL.toFilter("CROSSES(ATTR1, LINESTRING(1 2, 10 15))");
		//Filter filter = CQL.toFilter("ATTR1 BEFORE 2006-11-30T01:30:00Z/2006-12-31T01:30:00Z OR ATTR2 BEFORE 2006-11-30T01:30:00Z/2006-12-31T01:30:00Z");
//		Query query = new Query(
//				"type",
//				filter);

		Object newFilter = filter.accept(
				//new ExtractTimeFilterVisitor(),
				ExtractGeometryFilterVisitor.GEOMETRY_VISITOR,
				null);
		
		int i = 6;
		//System.out.println(CQL.toCQL(newFilter));
	}
	
	@Test
	public void testMoreComplexQuery()
			throws CQLException {
		
		Filter filter = CQL
				//.toFilter("ATTR1 < abs(ATTR2)");
				//.toFilter("ATTR1 AFTER 2006-11-30T01:30:00Z/P10D");
				//.toFilter("ATTR1 < (1 + ((2 / 3) * 4))");
				.toFilter("(NOT (a > 6)) or (b < 4 and a > 4)");
		
		CNFFilterVisitor cnf = new CNFFilterVisitor();
		Filter newFilter = (Filter)filter.accept(cnf, null);
		
		PropertyFilterVisitor vis = new PropertyFilterVisitor();
		And and = (And)newFilter;
		PropertyConstraintSet set = (PropertyConstraintSet)and.getChildren().get(0).accept(vis, null);
		
		System.out.println(CQL.toCQL(newFilter));
		System.out.println(CQL.toCQL(newFilter));
		
		
		
//		Query query = new Query(
//				"type",
//				filter);
//
//		PropertyFilterVisitor visitor = new PropertyFilterVisitor();
//
//		PropertyConstraintSet constraints = (PropertyConstraintSet) query.getFilter().accept(
//				visitor,
//				null);
//		printConstraints(constraints);
	}

	private void printConstraints(PropertyConstraintSet c) {
		
		System.out.println("Printing Constraints");
		for (Entry<ByteArrayId, FilterableConstraints> entry : c.getConstraints().entrySet()) {
			System.out.println(entry.getKey().toString() + " = " + entry.getValue().getClass());
		}
		
	}
	
	@Test
	public void testNumbersTypes()
			throws CQLException {
		Filter filter = CQL
				.toFilter("a < 9 and c = 12 and e >= 11 and f <= 12 and g > 13 and h between 4 and 6 and k > 4 and k < 6 and l >= 4 and l <= 6");
		Query query = new Query(
				"type",
				filter);

		PropertyFilterVisitor visitor = new PropertyFilterVisitor();

		PropertyConstraintSet constraints = (PropertyConstraintSet) query.getFilter().accept(
				visitor,
				null);
		NumberRangeFilter nf = (NumberRangeFilter) ((NumericLessThanConstraint) constraints
				.getConstraintsById(new ByteArrayId(
						"a"))).getFilter();
		assertTrue(nf.getLowerValue().doubleValue() == Double.MIN_VALUE);
		assertEquals(
				9,
				nf.getUpperValue().longValue());
		assertFalse(nf.isInclusiveHigh());
		assertTrue(nf.isInclusiveLow());

		nf = (NumberRangeFilter) ((NumericQueryConstraint) constraints.getConstraintsById(new ByteArrayId(
				"e"))).getFilter();
		assertEquals(
				11,
				nf.getLowerValue().longValue());
		assertTrue(nf.getUpperValue().doubleValue() == Double.MAX_VALUE);
		assertTrue(nf.isInclusiveHigh());
		assertTrue(nf.isInclusiveLow());

		nf = (NumberRangeFilter) ((NumericEqualsConstraint) constraints.getConstraintsById(new ByteArrayId(
				"c"))).getFilter();
		assertEquals(
				12,
				nf.getLowerValue().longValue());
		assertEquals(
				12,
				nf.getUpperValue().longValue());
		assertTrue(nf.isInclusiveHigh());
		assertTrue(nf.isInclusiveLow());

		nf = (NumberRangeFilter) ((NumericQueryConstraint) constraints.getConstraintsById(new ByteArrayId(
				"g"))).getFilter();
		assertEquals(
				13,
				nf.getLowerValue().longValue());
		assertTrue(nf.getUpperValue().doubleValue() == Double.MAX_VALUE);

		assertTrue(nf.isInclusiveHigh());
		assertFalse(nf.isInclusiveLow());

		nf = (NumberRangeFilter) ((NumericQueryConstraint) constraints.getConstraintsById(new ByteArrayId(
				"f"))).getFilter();
		assertEquals(
				12,
				nf.getUpperValue().longValue());
		assertTrue(nf.getLowerValue().doubleValue() == Double.MIN_VALUE);
		assertTrue(nf.isInclusiveHigh());
		assertTrue(nf.isInclusiveLow());

		nf = (NumberRangeFilter) ((NumericQueryConstraint) constraints.getConstraintsById(new ByteArrayId(
				"h"))).getFilter();
		assertEquals(
				4,
				nf.getLowerValue().longValue());
		assertEquals(
				6,
				nf.getUpperValue().longValue());
		assertTrue(nf.isInclusiveHigh());
		assertTrue(nf.isInclusiveLow());

		nf = (NumberRangeFilter) ((NumericQueryConstraint) constraints.getConstraintsById(new ByteArrayId(
				"k"))).getFilter();
		assertEquals(
				4,
				nf.getLowerValue().longValue());
		assertEquals(
				6,
				nf.getUpperValue().longValue());
		assertFalse(nf.isInclusiveHigh());
		assertFalse(nf.isInclusiveLow());

		nf = (NumberRangeFilter) ((NumericQueryConstraint) constraints.getConstraintsById(new ByteArrayId(
				"l"))).getFilter();
		assertEquals(
				4,
				nf.getLowerValue().longValue());
		assertEquals(
				6,
				nf.getUpperValue().longValue());
		assertTrue(nf.isInclusiveHigh());
		assertTrue(nf.isInclusiveLow());

	}

	@Test
	public void testTextTypes()
			throws CQLException {
		Filter filter = CQL.toFilter("b = '10'");
		Query query = new Query(
				"type",
				filter);

		PropertyFilterVisitor visitor = new PropertyFilterVisitor();

		PropertyConstraintSet constraints = (PropertyConstraintSet) query.getFilter().accept(
				visitor,
				null);
		TextExactMatchFilter tf = (TextExactMatchFilter) ((TextQueryConstraint) constraints
				.getConstraintsById(new ByteArrayId(
						"b"))).getFilter();
		assertEquals(
				"10",
				tf.getMatchValue());
		assertTrue(tf.isCaseSensitive());

	}
}
