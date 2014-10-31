package places;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

public class PlaceSearch {

	private final URL url;
	private final Map<String, String> queryValues;

	public PlaceSearch(String apiUrl, String query)
			throws MalformedURLException {
		url = new URL(apiUrl + query);
		queryValues = parseQuery(query);
	}

	public String getQueryValue(String parameter) {
		return queryValues.get(parameter);
	}

	// TODO handle errors
	public JSONObject doSearch(PrintWriter out) throws IOException {
		HttpURLConnection connection = (HttpsURLConnection) url
				.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));

		StringBuilder response = new StringBuilder();
		String line;
		while ((line = in.readLine()) != null) {
			out.println(line);
			response.append(line);
		}

		return new JSONObject(response.toString());
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
