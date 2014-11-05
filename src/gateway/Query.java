package gateway;

import java.util.ArrayList;
import java.util.List;

/**
 * A database query.
 */
public class Query {

	private final StringBuilder query;
	private final String querySuffix;
	private final List<String> parameters;

	public Query(String queryPrefix, String querySuffix) {
		this.query = new StringBuilder(queryPrefix);
		this.querySuffix = querySuffix;
		this.parameters = new ArrayList<String>();
	}

	/**
	 * Add the specified parameter with the specified value to the query.
	 *
	 * @param parameter
	 *            the parameter
	 *
	 * @param value
	 *            the value
	 */
	public void addParameter(String parameter, String value) {
		if (parameters.size() == 0) {
			query.append(" WHERE");
		}
		query.append(" " + parameter + "=?");
		parameters.add(value);
	}

	/**
	 * Get the query.
	 *
	 * @return The query.
	 */
	public String getQuery() {
		return query.toString() + querySuffix;
	}

	/**
	 * Get the query parameter list.
	 *
	 * @return The query parameter list
	 */
	public List<String> getParameters() {
		return parameters;
	}
}
