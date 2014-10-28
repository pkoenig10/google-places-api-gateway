package gateway;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Request {

	private final URL url;
	private final Map<String, String> queryValues;

	public Request(String apiUrl, String query) throws MalformedURLException {
		url = new URL(apiUrl + query);
		queryValues = parseQuery(query);
	}

	public String getQueryValue(String parameter) {
		return queryValues.get(parameter);
	}

	private static Map<String, String> parseQuery(String query) {
		Map<String, String> queryValues = new HashMap<String, String>();
		for (String item : query.split("&")) {
			String[] parameterValue = item.split("=");
			if (parameterValue.length != 2) {
				// TODO handle error
			}
			queryValues.put(parameterValue[0], parameterValue[1]);
		}
		return queryValues;
	}
}
