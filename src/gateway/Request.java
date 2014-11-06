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
		this.path = url.getPath();
		this.query = url.getQuery() != null ? url.getQuery() : "";
		this.parameters = parseQuery(query);
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
	 * Gets the URL query.
	 *
	 * @return The URL query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Gets the value of the specified query parameter.
	 *
	 * @param parameter
	 *            the query parameter
	 *
	 * @return The value of the specified query parameter
	 */
	public String get(String parameter) {
		return parameters.get(parameter);
	}

	/**
	 * Gets the mapping of query parameters to their values.
	 *
	 * @return The mapping of query parameters to their values.
	 */
	public Map<String, String> getParameters() {
		return parameters;
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
	 * @param query
	 *            the URL query
	 *
	 * @return A mapping of query parameters to their values.
	 */
	private static Map<String, String> parseQuery(String query) {
		Map<String, String> parameters = new HashMap<String, String>();

		for (String item : query.split("&")) {
			String[] parameterValue = item.split("=");
			if (parameterValue.length == 2) {
				parameters.put(parameterValue[0], parameterValue[1]);
			}
		}

		return parameters;
	}
}
