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

	public Query(Request request, String queryPrefix, String querySuffix) {
		this.query = new StringBuilder(queryPrefix);
		this.querySuffix = querySuffix;
		this.parameters = parseRequest(request);
	}

	/**
	 * Gets the database query.
	 *
	 * @return The database query
	 */
	public String getQuery() {
		return query.toString() + querySuffix;
	}

	/**
	 * Gets the value of the query parameter at the specified index.
	 *
	 * @param index
	 *            the index of the query parameter
	 *
	 * @return The value of the specified query parameter
	 */
	public String get(int index) {
		return parameters.get(index);
	}

	/**
	 * Gets the number of parameters in the query.
	 *
	 * @return The number of parameters in the query
	 */
	public int size() {
		return parameters.size();
	}

	/**
	 * Puts all of the API query parameters in a map.
	 *
	 * @param request
	 *            the client request
	 *
	 * @return A mapping of query parameters to their values.
	 */
	private List<String> parseRequest(Request request) {
		List<String> parameters = new ArrayList<String>();

		if (request.size() > 0) {
			query.append(" WHERE");
		}
		for (String parameter : request.getParameters().keySet()) {
			query.append(" " + parameter + "=?");
			parameters.add(request.get(parameter));
		}

		return parameters;
	}
}
