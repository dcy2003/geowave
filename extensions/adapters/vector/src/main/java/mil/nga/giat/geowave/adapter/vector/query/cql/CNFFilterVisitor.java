package mil.nga.giat.geowave.adapter.vector.query.cql;

import java.util.ArrayList;
import java.util.List;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;

/**
 * Filter Visitor converts a query into Conjunctive Normal Form
 * so that we can evaluate each piece for matching indexes and 
 * best cost.
 * https://en.wikipedia.org/wiki/Conjunctive_normal_form
 */
public class CNFFilterVisitor extends DuplicatingFilterVisitor {

	@Override
	public Object visit(And filter, Object extraData) {

		filter = (And) super.visit(filter, extraData);

		// The purpose of this code is really to just compress hierarchies of 
		// And's into one And expression with multiple children.  This will
		// help with constraints as the expressions are syntactically
		// equivalent either way.
		List<Filter> newChildren = new ArrayList<Filter>();
		for (Filter child : filter.getChildren()) {
			if (child instanceof And) {
				newChildren.addAll(((And) child).getChildren());
			} else {
				newChildren.add(child);
			}
		}
		return getFactory(extraData).and(newChildren);
	}

	@Override
	public Object visit(Or filter, Object extraData) {

		filter = (Or) super.visit(filter, extraData);

		// Ensure that there are only two expressions Ored together
		filter = groupOr(filter, extraData);

		// Execute the distributive property.
		Filter left = filter.getChildren().get(0);
		Filter right = filter.getChildren().get(1);

		if (left instanceof And) {
			return visit(distribute(left, right, extraData), extraData);
		}
		else if (right instanceof And) {
			return visit(distribute(right, left, extraData), extraData);
		}
		
		// The purpose of this code is really to just compress hierarchies of 
		// Or's into one Or expression with multiple children.  This will
		// help with constraints as the expressions are syntactically
		// equivalent either way.		
		List<Filter> newChildren = new ArrayList<Filter>();
		for (Filter child : filter.getChildren()) {
			if (child instanceof Or) {
				newChildren.addAll(((Or) child).getChildren());
			} else {
				newChildren.add(child);
			}
		}
		return getFactory(extraData).or(newChildren);
	};

	@Override
	public Object visit(Not filter, Object extraData) {
		// Double NOT
		// https://en.wikipedia.org/wiki/Double_negation#Double_negative_elimination
		if (filter.getFilter() instanceof Not) {
			Not childFilter = (Not) filter.getFilter();
			return childFilter.getFilter().accept(this, extraData);
		}

		// Push down NOT (Demorgan)
		// https://en.wikipedia.org/wiki/De_Morgan%27s_laws
		if (filter.getFilter() instanceof And) {
			And andFilter = groupAnd((And) filter.getFilter(), extraData);
			// De morgan it.
			return getFactory(extraData)
					.or(
							getFactory(extraData)
								.not(andFilter.getChildren().get(0)),
							getFactory(extraData)
								.not(andFilter.getChildren().get(1)))
					.accept(this, extraData);
		} else if (filter.getFilter() instanceof Or) {
			Or orFilter = groupOr((Or) filter.getFilter(), extraData);
			// De morgan it.
			return getFactory(extraData)
					.and(
							getFactory(extraData)
								.not(orFilter.getChildren().get(0)),
							getFactory(extraData)
								.not(orFilter.getChildren().get(1)))
					.accept(this, extraData);
		} else {
			return super.visit(filter, extraData);
		}
	}

	/**
	 * This function changes a collection of tons of Ors into a hierarchy of
	 * only two expressions being Ored together.
	 * @param filter
	 * @param extraData
	 * @return
	 */
	private Or groupOr(Or filter, Object extraData) {
		if (filter.getChildren().size() > 2) {
			List<Filter> newChildren = new ArrayList<Filter>(filter.getChildren());
			return getFactory(extraData)
					.or(
							newChildren.remove(0), 
							getFactory(extraData).or(newChildren)
			);
		}
		return filter;
	}

	/**
	 * This function just changes a collection of tons of Ands into a hierarchy
	 * of only two expressions being Anded together.
	 * @param filter
	 * @param extraData
	 * @return
	 */
	private And groupAnd(And filter, Object extraData) {
		if (filter.getChildren().size() > 2) {
			List<Filter> newChildren = new ArrayList<Filter>(filter.getChildren());
			return getFactory(extraData)
					.and(
							newChildren.remove(0), 
							getFactory(extraData).and(newChildren)
			);
		}
		return filter;
	}
	
	/**
	 * Distributive property of boolean logic.
	 * https://en.wikipedia.org/wiki/Distributive_property
	 * @param left
	 * @param right
	 * @param extraData
	 * @return
	 */
	public And distribute(Filter left, Filter right, Object extraData) {
		And leftAnd = groupAnd((And) left, extraData);
		return getFactory(extraData)
				.and(
						getFactory(extraData)
							.or(leftAnd.getChildren().get(0), right),
						getFactory(extraData)
							.or(leftAnd.getChildren().get(1), right)
		);
	}

}
