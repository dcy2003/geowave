package mil.nga.giat.geowave.adapter.vector.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.geotools.data.Query;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTS;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import mil.nga.giat.geowave.core.geotime.store.query.SpatialConstraints;
import mil.nga.giat.geowave.core.geotime.store.query.SpatialConstraintsSet;

public class ExtractGeometryFilterVisitorTest
{
	final ExtractGeometryFilterVisitor visitorWithDescriptor = (ExtractGeometryFilterVisitor) ExtractGeometryFilterVisitor.GEOMETRY_VISITOR;

	@Test
	public void testDWithin()
			throws CQLException,
			TransformException,
			ParseException {

		Filter filter = CQL.toFilter("DWITHIN(geom1, POINT(-122.7668 0.4979), 233.7, meters)");
		Query query = new Query(
				"type",
				filter);

		SpatialConstraints geometry = (SpatialConstraints) query.getFilter().accept(
				visitorWithDescriptor,
				null);
		assertNotNull(geometry);
		assertEquals("geom1", geometry.getName());		
		for (Coordinate coord : geometry.getGeometry().getCoordinates()) {

			assertEquals(
					233.7,
					JTS.orthodromicDistance(
							coord,
							new Coordinate(
									-122.7668,
									0.4979),
							GeoWaveGTDataStore.DEFAULT_CRS),
					2);
		}
	}

	@Test
	public void testDWithinDateLine()
			throws CQLException,
			TransformException,
			ParseException {

		Filter filter = CQL.toFilter("DWITHIN(geom, POINT(179.9998 0.79), 13.7, kilometers)");
		Query query = new Query(
				"type",
				filter);

		SpatialConstraints geometry = (SpatialConstraints) query.getFilter().accept(
				visitorWithDescriptor,
				null);
		assertNotNull(geometry);
		assertEquals("geom", geometry.getName());
		for (Coordinate coord : geometry.getGeometry().getCoordinates()) {

			assertEquals(
					13707.1,
					JTS.orthodromicDistance(
							coord,
							new Coordinate(
									179.9999,
									0.79),
							GeoWaveGTDataStore.DEFAULT_CRS),
					2000);
		}
	}
	
	@Test
	public void testBbox() throws CQLException {
		Filter filter = CQL.toFilter("BBOX(geom2,-90, 40, -60, 45)");
		SpatialConstraints geometry = (SpatialConstraints) filter.accept(
				visitorWithDescriptor,
				null);
		assertNotNull(geometry);
		assertEquals("geom2", geometry.getName());	
		assertTrue(geometry.getGeometry() instanceof Polygon);
		Coordinate[] coords = geometry.getGeometry().getCoordinates();
		assertEquals(-90, coords[0].getOrdinate(0), 0.0001);
	}
	
	@Test
	public void testAndSame() throws CQLException {
		Filter filter = CQL.toFilter("BBOX(geom2,-85, 40, -60, 45) AND BBOX(geom2,-90, 40, -60, 45)");
		SpatialConstraintsSet geometrySet = (SpatialConstraintsSet) filter.accept(
				visitorWithDescriptor,
				null);
		assertNotNull(geometrySet);
		assertEquals(1, geometrySet.getSet().size());
		SpatialConstraints geometry = geometrySet.getConstraintsFor("geom2");
		assertEquals("geom2", geometry.getName());	
		assertTrue(geometry.getGeometry() instanceof Polygon);
		Coordinate[] coords = geometry.getGeometry().getCoordinates();
		assertEquals(-85, coords[0].getOrdinate(0), 0.0001);
	}
	
	@Test
	public void testAndDifferent() throws CQLException {
		Filter filter = CQL.toFilter("BBOX(geom1,-85, 40, -60, 45) AND BBOX(geom2,-90, 40, -60, 45)");
		SpatialConstraintsSet geometrySet = (SpatialConstraintsSet) filter.accept(
				visitorWithDescriptor,
				null);
		assertNotNull(geometrySet);
		{
			SpatialConstraints geometry = geometrySet.getConstraintsFor("geom2");
			assertEquals("geom2", geometry.getName());	
			assertTrue(geometry.getGeometry() instanceof Polygon);
			Coordinate[] coords = geometry.getGeometry().getCoordinates();
			assertEquals(-90, coords[0].getOrdinate(0), 0.0001);
		}
		{
			SpatialConstraints geometry = geometrySet.getConstraintsFor("geom1");
			assertEquals("geom1", geometry.getName());	
			assertTrue(geometry.getGeometry() instanceof Polygon);
			Coordinate[] coords = geometry.getGeometry().getCoordinates();
			assertEquals(-85, coords[0].getOrdinate(0), 0.0001);
		}
	}
	
	@Test
	public void testOrSame() throws CQLException {
		Filter filter = CQL.toFilter("BBOX(geom2,-85, 40, -60, 45) OR BBOX(geom2,-90, 40, -60, 45)");
		SpatialConstraintsSet geometrySet = (SpatialConstraintsSet) filter.accept(
				visitorWithDescriptor,
				null);
		assertNotNull(geometrySet);
		assertEquals(1, geometrySet.getSet().size());
		SpatialConstraints geometry = geometrySet.getConstraintsFor("geom2");
		assertEquals("geom2", geometry.getName());	
		assertTrue(geometry.getGeometry() instanceof Polygon);
		Coordinate[] coords = geometry.getGeometry().getEnvelope().getCoordinates();
		assertEquals(-90, coords[0].getOrdinate(0), 0.0001);
	}
	
	@Test
	public void testOrDifferent() throws CQLException {
		Filter filter = CQL.toFilter("BBOX(geom1,-85, 40, -60, 45) OR BBOX(geom2,-90, 40, -60, 45)");
		SpatialConstraintsSet geometrySet = (SpatialConstraintsSet) filter.accept(
				visitorWithDescriptor,
				null);
		assertNotNull(geometrySet);
		{
			SpatialConstraints geometry = geometrySet.getConstraintsFor("geom2");
			assertEquals("geom2", geometry.getName());	
			assertTrue(geometry.getGeometry() instanceof Polygon);
			Coordinate[] coords = geometry.getGeometry().getEnvelope().getCoordinates();
			assertEquals(-90, coords[0].getOrdinate(0), 0.0001);
		}
		{
			SpatialConstraints geometry = geometrySet.getConstraintsFor("geom1");
			assertEquals("geom1", geometry.getName());	
			assertTrue(geometry.getGeometry() instanceof Polygon);
			Coordinate[] coords = geometry.getGeometry().getEnvelope().getCoordinates();
			assertEquals(-85, coords[0].getOrdinate(0), 0.0001);
		}
	}

	@Test
	public void testGetConstraints() throws CQLException {
		Filter filter = CQL.toFilter("BBOX(geom2,-90, 40, -60, 45) AND BBOX(geom1,-85, 40, -60, 45)");
		
		SpatialConstraintsSet set = 
				ExtractGeometryFilterVisitor.getConstraints(filter, GeoWaveGTDataStore.DEFAULT_CRS);
		
		assertFalse(set.isInfinite());
		assertEquals(2, set.getSet().size());

		SpatialConstraints geometry = set.getConstraintsFor("geom2");
		
		assertNotNull(geometry);
		assertEquals("geom2", geometry.getName());	
		assertTrue(geometry.getGeometry() instanceof Polygon);
		Coordinate[] coords = geometry.getGeometry().getCoordinates();
		assertEquals(-90, coords[0].getOrdinate(0), 0.0001);
	}
}
