package gateway;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A client request.
 */
public class Request {

	private final String path;
	private final String query;
	private final Map<String, String> parameters;

	public Request(String urlPath) throws MalformedURLException {
		URL url = new URL("http", "localhost", urlPath);
		path = url.getPath();
		query = url.getQuery() != null ? url.getQuery() : "";
		parameters = parseQuery(query);
	}

	/**
	 * Get the URL path.
	 *
	 * @return The URL path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get the URL query.
	 *
	 * @return The URL query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Get the value of the specified query parameter.
	 *
	 * @param field
	 *            the query field
	 *
	 * @return The value of the specified query parameter
	 */
	public String getParameter(String parameter) {
		return parameters.get(parameter);
	}

	/**
	 * Puts all of the API query parameters in a map.
	 *
	 * @param query
	 *            the URL query
	 *
	 * @return A mapping of query parameters to their values.
	 */
	private static Map<String, String> parseQuery(String query) {
		Map<String, String> parameters = new HashMap<String, String>();

		if (query != null) {
			for (String item : query.split("&")) {
				String[] fieldValue = item.split("=");
				if (fieldValue.length == 2) {
					parameters.put(fieldValue[0], fieldValue[1]);
				}
			}
		}

		return parameters;
	}
}
