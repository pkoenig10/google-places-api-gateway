package places;

import gateway.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A Google Place Search API query.
 */
public class PlaceSearch extends Request {

	private static final String API_HOSTNAME = "maps.googleapis.com";

	private final String apiPath;

	public PlaceSearch(String apiPath, String query) {
		super(query);
		this.apiPath = apiPath + query;
	}

	/**
	 * Execute a Google Place Search API query and forward the response to the
	 * client.
	 *
	 * @param clientout
	 *            a {@link PrintWriter} to the client output stream
	 *
	 * @return A {@link JSONArray} containing the results of the query
	 *
	 * @throws IOException
	 *             if the query could not be completed successfully
	 */
	public JSONObject doSearch(PrintWriter clientOut) throws IOException {
		SSLSocket socket = null;

		try {
			// Open a connection to the API server
			socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(
					API_HOSTNAME, 443);

			// Send the API HTTP request
			PrintWriter apiOut = new PrintWriter(new OutputStreamWriter(
					socket.getOutputStream()), true);
			apiOut.println("GET " + apiPath + " HTTP/1.0");
			apiOut.println();

			// Read and forward the response
			BufferedReader in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;

			// Read and forward the HTTP response headers
			while ((line = in.readLine()) != null) {
				clientOut.println(line);
				if (line.isEmpty()) {
					break;
				}
			}

			// Read and forward the HTTP response body
			while ((line = in.readLine()) != null) {
				clientOut.println(line);
				response.append(line);
			}

			return new JSONObject(response.toString());

		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				// Ignore because we're about to exit anyway.
			}
		}
	}
}
