package gateway;

import java.util.HashMap;
import java.util.Map;

/**
 * A client request.
 */
public class Request {

	private final Map<String, String> parameters;

	public Request(String query) {
		parameters = parseQuery(query);
	}

	/**
	 * Get the value of the specified query field.
	 *
	 * @param field
	 *            the query field
	 *
	 * @return The value of the specified query field
	 */
	public String getParameter(String field) {
		return parameters.get(field);
	}

	/**
	 * Puts all of the API query parameters in a map.
	 *
	 * @param query
	 *            the URL query
	 *
	 * @return A mapping of query fields to their values.
	 */
	private static Map<String, String> parseQuery(String query) {
		Map<String, String> parameters = new HashMap<String, String>();

		for (String item : query.split("&")) {
			String[] fieldValue = item.split("=");
			if (fieldValue.length == 2) {
				parameters.put(fieldValue[0], fieldValue[1]);
			}
		}

		return parameters;
	}
}
