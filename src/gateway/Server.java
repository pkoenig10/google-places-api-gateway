package gateway;

import static places.PlaceConstants.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.AuthenticationException;

import org.json.JSONArray;
import org.json.JSONObject;

import places.PlaceResult;
import places.PlaceSearch;

/**
 * A gateway server for the Google Place Search API that supports user
 * authentication and logs and analyzes search queries and results.
 */
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

	// Path to the favicon image file
	private static final String FAVICON_PATH = "res/favicon.ico";

	// Buffer size for writing data
	private static final int BUF_SIZE = 8192;

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
			try {
				Socket clientSocket = serverSocket.accept();
				executor.execute(new ClientHandler(clientSocket));

			} catch (IOException e) {
				// TODO log errors
				e.printStackTrace();
				break;
			} catch (AuthenticationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			serverSocket.close();
		} catch (IOException e) {
			// Ignore because we're about to exit anyway.
		}
	}

	// TODO check all throws declarations
	class ClientHandler implements Runnable {

		private final Socket socket;

		private final String username;

		private final UUID sessionId;

		public ClientHandler(Socket socket, String username, String password)
				throws AuthenticationException {
			this.socket = socket;
			if (username != null && !validateUser(username, password)) {
				throw new AuthenticationException("Invalid credentials");
			}
			this.username = username;
			this.sessionId = UUID.randomUUID();
		}

		public ClientHandler(Socket socket) throws AuthenticationException {
			this(socket, null, null);
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
					out.print(getQueryErrorResponse(RESPONSE_STATUS_GATEWAY_INVALID_REQUEST));
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

				case GATEWAY_PATH_FAVICON:
					doFavicon();

				default:
					out.print(getQueryErrorResponse(RESPONSE_STATUS_GATEWAY_INVALID_URL));
				}

			} catch (IOException e) {
				// TODO log errors
				e.printStackTrace();
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
		 * Executes a Google Place Search API query and forwards the response to
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
		 * Writes the favicon to the client.
		 */
		private void doFavicon() {
			FileInputStream favicon = null;

			try {
				// Get the output stream for the socket
				BufferedOutputStream out = new BufferedOutputStream(
						socket.getOutputStream());

				// Get the input stream for the favicon image file
				favicon = new FileInputStream(new File(FAVICON_PATH));

				// Write the favicon to the client
				byte[] buf = new byte[BUF_SIZE];
				int len;
				while ((len = favicon.read(buf, 0, buf.length)) > 0) {
					out.write(buf, 0, len);
				}
				out.flush();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (favicon != null) {
						favicon.close();
					}
				} catch (IOException e) {
					// Ignore because we're about to exit anyway.
				}
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
				statement.setString(2, username);
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
					statement.setString(2, username);
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

		/**
		 * Executes the specified SQL query on the database of searches and
		 * results.
		 *
		 * @param query
		 *            the query
		 *
		 * @return A {@link JSONObject} containing the results of the query
		 */
		private JSONObject executeQuery(String query) {
			Connection connection = null;
			Statement statement = null;
			ResultSet resultSet = null;

			try {
				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the query
				statement = connection.createStatement();
				resultSet = statement.executeQuery(query);

				return getQueryResponse(resultSet);

			} catch (SQLException e) {
				return getQueryErrorResponse(RESPONSE_STATUS_GATEWAY_SQL_ERROR);
			} finally {
				try {
					if (resultSet != null) {
						resultSet.close();
					}
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
		}

		/**
		 * Adds a user with the specified username and password as an authorized
		 * user.
		 *
		 * @param username
		 *            the username
		 *
		 * @param password
		 *            the password
		 *
		 * @return True if the user was successfully added, false otherwise
		 */
		private boolean addUser(String username, String password) {
			Connection connection = null;
			PreparedStatement statement = null;

			try {
				// Get a new salt and hash the password
				byte[] salt = getSalt();
				byte[] hashedPassword = hashPassword(password, salt);

				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the update to add the user
				statement = connection.prepareStatement(ADD_USER);
				statement.setString(1, username);
				statement.setString(2, encode(salt));
				statement.setString(3, encode(hashedPassword));
				statement.executeUpdate();

			} catch (SQLException | NoSuchAlgorithmException e) {
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
		 * Validates a user to allow access to the gateway.
		 *
		 * @param username
		 *            the username
		 *
		 * @param password
		 *            the password
		 *
		 * @return True if the credentials are valid, false otherwise
		 */
		private boolean validateUser(String username, String password) {
			Connection connection = null;
			PreparedStatement statement = null;
			ResultSet result = null;

			try {
				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the query to get the stored salt and password
				statement = connection.prepareStatement(VALIDATE_USER);
				statement.setString(1, username);
				result = statement.executeQuery();

				// Check that the stored hashed password matches the hash of the
				// given password
				if (result.next()) {
					byte[] salt = decode(result.getString(SALT));
					String encodedHashedPassword = result.getString(PASSHASH);

					if (encode(hashPassword(password, salt)).equals(
							encodedHashedPassword)) {
						return true;
					}
				}

			} catch (SQLException | NoSuchAlgorithmException e) {
				return false;
			} finally {
				try {
					if (result != null) {
						result.close();
					}
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

			return false;
		}
	}

	/**
	 * Hashes the specified password using SHA-256 with the specified salt.
	 *
	 * @param password
	 *            the password
	 *
	 * @param salt
	 *            the salt
	 *
	 * @return The hashed password
	 *
	 * @exception NoSuchAlgorithmException
	 *                if the hashing algorithm is not supported
	 */
	private static byte[] hashPassword(String password, byte[] salt)
			throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.reset();
		digest.update(salt);
		return digest.digest(password.getBytes());
	}

	/**
	 * Generates a new random 32 byte salt.
	 *
	 * @return The salt
	 */
	private static byte[] getSalt() {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[32];
		random.nextBytes(salt);
		return salt;
	}

	/**
	 * Encodes the specified byte array into a String using the {@link Base64}
	 * encoding scheme.
	 *
	 * @param src
	 *            the byte array to encode
	 *
	 * @return A String containing the resulting Base64 encoded characters
	 */
	private static String encode(byte[] src) {
		return Base64.getEncoder().encodeToString(src);
	}

	/**
	 * Decodes a String into a byte array using the {@link Base64} encoding
	 * scheme.
	 *
	 * @param src
	 *            the string to decode
	 *
	 * @return A byte array containing the decoded bytes.
	 */
	private static byte[] decode(String src) {
		return Base64.getDecoder().decode(src);
	}

	/**
	 * Convert a {@link ResultSet} from an SQL query to a {@link JSONObject} .
	 *
	 * @param resultSet
	 *            the result set returned from the query
	 *
	 * @return A JSONObject containing the results of the query
	 */
	private static JSONObject getQueryResponse(ResultSet resultSet) {
		try {
			JSONObject jsonResponse = new JSONObject();
			JSONArray jsonResults = new JSONArray();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

			while (resultSet.next()) {
				JSONObject jsonResult = new JSONObject();
				int cols = resultSetMetaData.getColumnCount();

				// TODO check order
				for (int i = cols; i > 0; i--) {
					jsonResult.put(resultSetMetaData.getColumnLabel(i)
							.toLowerCase(), resultSet.getObject(i));
				}

				jsonResults.put(jsonResult);
			}

			jsonResponse.put(RESPONSE_STATUS, RESPONSE_STATUS_OK);
			jsonResponse.put(RESPONSE_RESULTS, jsonResults);

			return jsonResponse;
		} catch (SQLException e) {
			return getQueryErrorResponse(RESPONSE_STATUS_GATEWAY_JSON_ERROR);
		}
	}

	/**
	 * Returns a {@link JSONObject} with empty results and the specified error
	 * status.
	 *
	 * @param status
	 *            the error status
	 *
	 * @return A {@link JSONObject} with empty results and the specified error
	 *         status
	 */
	private static JSONObject getQueryErrorResponse(String status) {
		JSONObject jsonResponse = new JSONObject();

		jsonResponse.put(RESPONSE_STATUS, status);
		jsonResponse.put(RESPONSE_RESULTS, new JSONArray());

		return jsonResponse;
	}

	public static void main(String[] args) {
		Server server = new Server(12345,
				"jdbc:postgresql://71.199.115.219/googleplaces",
				"googleplaces", "googleplaces");
		server.start();
	}
}