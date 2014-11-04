package gateway;

import static places.PlaceConstants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.AuthenticationException;

import org.json.JSONArray;
import org.json.JSONObject;

import places.PlaceResult;
import places.PlaceSearch;

public class Server extends Thread {

	// URL paths used by the client to interact with the gateway
	private static final String GATEWAY_PATH_NEARBY_SEARCH = "/google-places-api-gateway/nearbysearch";
	private static final String GATEWAY_PATH_TEXT_SEARCH = "/google-places-api-gateway/textsearch";
	private static final String GATEWAY_PATH_RADAR_SEARCH = "/google-places-api-gateway/radarsearch";
	private static final String GATEWAY_PATH_ADD_USER = "/google-places-api-gateway/adduser";
	private static final String GATEWAY_PATH_QUERY = "/google-places-api-gateway/query";
	private static final String GATEWAY_PATH_FAVICON = "/favicon.ico";

	// URLs used by the gateway to interact with the API
	private static final String API_PATH_NEARBY_SEARCH = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
	private static final String API_PATH_TEXT_SEARCH = "https://maps.googleapis.com/maps/api/place/textsearch/json?";
	private static final String API_PATH_RADAR_SEARCH = "https://maps.googleapis.com/maps/api/place/radarsearch/json?";

	// SQL statements for updating and querying the database
	private static final String INSERT_SEARCH = "INSERT INTO searches (session_id, username, query, location, radius, keyword, language, "
			+ "minprice, maxprice, name, opennow, rankby, types, pagetoken, zagatselected) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
	private static final String INSERT_RESULT = "INSERT INTO results (session_id, username, place_id, lat, lng) VALUES (?, ?, ?, ?, ?);";
	private static final String ADD_USER = "INSERT INTO users (username, salt, passhash) VALUES (?, ?, ?);";
	private static final String VALIDATE_USER = "SELECT salt, passhash FROM users WHERE username = ? LIMIT 1;";

	// Columns in the user table
	private static final String SALT = "salt";
	private static final String PASSHASH = "passhash";

	// JSON response fields and value
	private static final String RESPONSE_RESULTS = "results";
	private static final String RESPONSE_STATUS = "status";
	private static final String RESPONSE_STATUS_OK = "OK";
	private static final String RESPONSE_STATUS_GATEWAY_INVALID_REQUEST = "GATEWAY_INVALID_REQUEST";
	private static final String RESPONSE_STATUS_GATEWAY_INVALID_URL = "GATEWAY_INVALID_URL";
	private static final String RESPONSE_STATUS_GATEWAY_JSON_ERROR = "GATEWAY_JSON_ERROR";
	private static final String RESPONSE_STATUS_GATEWAY_SQL_ERROR = "GATEWAY_SQL_ERROR";

	// Number of threads to be used
	private static final int NUM_THREADS = 10;

	// The port number
	private final int port;

	// Database credentials
	private final String dbUrl;
	private final String dbUser;
	private final String dbPassword;

	private final ExecutorService executor;

	public Server(int port, String dbUrl, String dbUser, String dbPassword) {
		this.port = port;
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		executor = Executors.newFixedThreadPool(NUM_THREADS);
	}

	public Server(int port) {
		this(port, null, null, null);
	}

	@Override
	public void run() {
		// Create the server socket
		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			return;
		}

		// Accept incoming connections, handle them on a background thread,
		// and immediately begin listening for other incoming client
		// connections.
		while (true) {
			Socket clientSocket = null;

			try {
				clientSocket = serverSocket.accept();
				executor.execute(new ClientHandler(clientSocket));

			} catch (IOException e) {
				// TODO log errors
				break;
			} catch (AuthenticationException e) {
				// TODO Auto-generated catch block
				break;
			} finally {
				try {
					if (clientSocket != null) {
						clientSocket.close();
					}
				} catch (IOException e) {
					// Ignore because we're about to exit anyway.
				}
			}
		}

		try {
			serverSocket.close();
		} catch (IOException e) {
			// Ignore because we're about to exit anyway.
		}
	}

	class ClientHandler implements Runnable {

		private final Socket socket;

		private final UUID sessionId;

		public ClientHandler(Socket socket) throws AuthenticationException {
			this.socket = socket;
			this.sessionId = UUID.randomUUID();
		}

		@Override
		public void run() {
			try {
				// Get input and output streams for the socket
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(),
						true);

				// Parse the client request and ensure it is a valid HTTP
				// request
				String[] request = in.readLine().split(" ");
				if (request.length != 3
						|| !request[0].equals("GET")
						|| (!request[2].equals("HTTP/1.0") && !request[2]
								.equals("HTTP/1.1"))) {
					// TODO handle error
					return;
				}

				// Handle all valid paths for the request url
				URL requestUrl = new URL("http", "localhost", request[1]);

				switch (requestUrl.getPath()) {

				case GATEWAY_PATH_NEARBY_SEARCH:
					doPlaceSearch(new PlaceSearch(API_PATH_NEARBY_SEARCH,
							requestUrl.getQuery()), out);

				case GATEWAY_PATH_TEXT_SEARCH:
					doPlaceSearch(new PlaceSearch(API_PATH_TEXT_SEARCH,
							requestUrl.getQuery()), out);

				case GATEWAY_PATH_RADAR_SEARCH:
					doPlaceSearch(new PlaceSearch(API_PATH_RADAR_SEARCH,
							requestUrl.getQuery()), out);

				default:
					// TODO handle this case
				}

			} catch (IOException e) {
				// TODO log errors
				return;
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

		/**
		 * Execute a Google Place Search API query and forwards the response to
		 * the client.
		 *
		 * @param placesSearch
		 *            the search query
		 *
		 * @param out
		 *            the client output stream
		 *
		 * @throws IOException
		 *             if the query could not be completed successfully
		 */
		private void doPlaceSearch(PlaceSearch placesSearch, PrintWriter out)
				throws IOException {
			// Execute the search and forward the response to the client
			JSONObject jsonResponse = placesSearch.doSearch(out);

			// Write the search and the results to the database
			if (jsonResponse.getString(RESPONSE_STATUS).equals(
					RESPONSE_STATUS_OK)) {
				writeSearch(placesSearch);
				writeResults(jsonResponse.getJSONArray(RESPONSE_RESULTS));
			}
		}

		/**
		 * Writes a search to the database.
		 *
		 * @param search
		 *            the performed search
		 *
		 * @return True of the search was written successfully, false otherwise
		 */
		private boolean writeSearch(PlaceSearch search) {
			Connection connection = null;
			PreparedStatement statement = null;

			try {
				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the update
				statement = connection.prepareStatement(INSERT_SEARCH);
				statement.setObject(1, sessionId);
				statement.setString(2, null); // Usernames not yet implemented
				statement.setString(3, search.getParameter(QUERY));
				statement.setString(4, search.getParameter(LOCATION));
				statement.setString(5, search.getParameter(RADIUS));
				statement.setString(6, search.getParameter(KEYWORD));
				statement.setString(7, search.getParameter(LANGUAGE));
				statement.setString(8, search.getParameter(MINPRICE));
				statement.setString(9, search.getParameter(MAXPRICE));
				statement.setString(10, search.getParameter(NAME));
				statement.setString(11, search.getParameter(OPENNOW));
				statement.setString(12, search.getParameter(RANKBY));
				statement.setString(13, search.getParameter(TYPES));
				statement.setString(14, search.getParameter(PAGETOKEN));
				statement.setString(15, search.getParameter(ZAGATSELECTED));
				statement.executeUpdate();

			} catch (SQLException e) {
				return false;
			} finally {
				try {
					if (statement != null) {
						statement.close();
					}
					if (connection != null) {
						connection.close();
					}
				} catch (SQLException e) {
					// Do nothing because we are exiting
				}
			}

			return true;
		}

		/**
		 * Writes the results of a search to the database.
		 *
		 * @param results
		 *            a {@link JSONArray} containing the results of the query
		 *
		 * @return True of the results were written successfully, false
		 *         otherwise
		 */
		private boolean writeResults(JSONArray results) {
			Connection connection = null;
			PreparedStatement statement = null;

			try {
				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the updates
				// TODO refactor classes?
				statement = connection.prepareStatement(INSERT_RESULT);
				for (int i = 0; i < results.length(); i++) {
					PlaceResult result = new PlaceResult(
							results.getJSONObject(i));
					statement.setObject(1, sessionId);
					statement.setString(2, null); // Usernames not yet
													// implemented
					statement.setString(3, result.getPlaceId());
					statement.setDouble(4, result.getLatitude());
					statement.setDouble(5, result.getLongitude());
					statement.executeUpdate();
				}

			} catch (SQLException e) {
				return false;
			} finally {
				try {
					if (statement != null) {
						statement.close();
					}
					if (connection != null) {
						connection.close();
					}
				} catch (SQLException e) {
					// Do nothing because we are exiting
				}
			}

			return true;
		}
	}

	public static void main(String[] args) {
		Server server = new Server(12345);
		server.start();
	}
}
