package gateway;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A gateway server for the Google Place Search API that supports user
 * authentication and logs and analyzes search queries and results.
 */
public class Gateway extends Thread {

	// Properties file with database information
	private static final String PROPERTIES = "gateway.properties";
	private static final String DB_URL = "dbUrl";
	private static final String DB_USERNAME = "dbUsername";
	private static final String DB_PASSWORD = "dbPassword";

	// URL paths used by the client to interact with the gateway
	private static final String GATEWAY_PATH_NEARBY_SEARCH = "/google-places-api-gateway/nearbysearch";
	private static final String GATEWAY_PATH_TEXT_SEARCH = "/google-places-api-gateway/textsearch";
	private static final String GATEWAY_PATH_RADAR_SEARCH = "/google-places-api-gateway/radarsearch";
	private static final String GATEWAY_PATH_ADD_USER = "/google-places-api-gateway/adduser";
	private static final String GATEWAY_PATH_SEARCH_QUERY = "/google-places-api-gateway/searchquery";
	private static final String GATEWAY_PATH_RESULT_QUERY = "/google-places-api-gateway/resultquery";

	// URL hostname and paths used by the gateway to interact with the API
	private static final String API_HOSTNAME = "maps.googleapis.com";
	private static final String API_PATH_NEARBY_SEARCH = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
	private static final String API_PATH_TEXT_SEARCH = "https://maps.googleapis.com/maps/api/place/textsearch/json?";
	private static final String API_PATH_RADAR_SEARCH = "https://maps.googleapis.com/maps/api/place/radarsearch/json?";

	// SQL statements for updating and querying the database
	private static final String INSERT_SEARCH = "INSERT INTO searches (sessionid, timestamp, username, query, location, radius, keyword, language, "
			+ "minprice, maxprice, name, opennow, rankby, types, pagetoken, zagatselected) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
	private static final String INSERT_RESULT = "INSERT INTO results (sessionid, timestamp, username, placeid, lat, lng) VALUES (?, ?, ?, ?, ?, ?);";
	private static final String ADD_USER = "INSERT INTO users (username, salt, passhash) VALUES (?, ?, ?);";
	private static final String VALIDATE_USER = "SELECT salt, passhash FROM users WHERE username = ? LIMIT 1;";
	private static final String SEARCH_QUERY_PREFIX = "SELECT sessionid, timestamp, username, query, location, radius, keyword, language, "
			+ "minprice, maxprice, name, opennow, rankby, types, pagetoken, zagatselected FROM SEARCHES";
	private static final String RESULT_QUERY_PREFIX = "SELECT sessionid, timestamp, username, placeid, lat, lng FROM RESULTS";
	private static final String QUERY_SUFFIX = " ORDER BY timestamp DESC LIMIT 500;";

	// URL query parameters for the gateway
	private static final String MY_USERNAME = "myusername";
	private static final String MY_PASSWORD = "mypassword";
	private static final String NEW_USERNAME = "newusername";
	private static final String NEW_PASSWORD = "newpassword";

	// URL query parameters for the Google Place Search API
	private static final String QUERY = "query";
	private static final String LOCATION = "location";
	private static final String RADIUS = "radius";
	private static final String KEYWORD = "keyword";
	private static final String LANGUAGE = "language";
	private static final String MINPRICE = "minprice";
	private static final String MAXPRICE = "maxprice";
	private static final String NAME = "name";
	private static final String OPENNOW = "opennow";
	private static final String RANKBY = "rankby";
	private static final String TYPES = "types";
	private static final String PAGETOKEN = "pagetoken";
	private static final String ZAGATSELECTED = "zagatselected";

	// Database fields
	private static final String SALT = "salt";
	private static final String PASSHASH = "passhash";

	// JSON response fields and value
	private static final String RESPONSE_RESULTS = "results";
	private static final String RESPONSE_STATUS = "status";
	private static final String RESPONSE_STATUS_OK = "OK";
	private static final String RESPONSE_STATUS_GATEWAY_INVALID_REQUEST = "GATEWAY_INVALID_REQUEST";
	private static final String RESPONSE_STATUS_GATEWAY_INVALID_URL = "GATEWAY_INVALID_URL";
	private static final String RESPONSE_STATUS_GATEWAY_AUTHENTICATION_FAILED = "GATEWAY_AUTHENTICATION_FAILED";
	private static final String RESPONSE_STATUS_SEARCH_ERROR = "GATEWAY_SEARCH_ERROR";
	private static final String RESPONSE_STATUS_GATEWAY_QUERY_ERROR = "GATEWAY_QUERY_ERROR";
	private static final String RESPONSE_STATUS_GATEWAY_ADD_USER_ERROR = "GATEWAY_AD_USER_ERROR";
	private static final String RESPONSE_STATUS_GATEWAY_UNKNOWN_ERROR = "GATEWAY_UNKNOWN_ERROR";

	// Number of threads to be used
	private static final int NUM_THREADS = 10;

	// If the gateway allows users who do not provide credentials
	private static final boolean ALLOW_ANON_USERS = true;

	private final int port;

	private final String dbUrl;
	private final String dbUser;
	private final String dbPassword;

	private final ExecutorService executor;

	public Gateway(int port, String dbUrl, String dbUser, String dbPassword) {
		this.port = port;
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		executor = Executors.newFixedThreadPool(NUM_THREADS);
	}

	public Gateway(int port) {
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

		Log.i(String.format("Starting gateway on port %d", port));

		// Accept incoming connections, handle them on a background thread,
		// and immediately begin listening for other incoming client
		// connections.
		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				Log.i(String.format("Accepted connection from %s:%d",
						clientSocket.getInetAddress().getHostAddress(),
						clientSocket.getPort()));
				executor.execute(new ClientHandler(clientSocket, System
						.currentTimeMillis()));

			} catch (IOException e) {
				Log.e("Exception when accepting connection", e);
				break;
			}
		}

		try {
			serverSocket.close();
		} catch (IOException e) {
			// Do nothing because we are exiting
			Log.e("Exception when closing server socket", e);
		}
	}

	/**
	 * A handler for a client interaction.
	 */
	class ClientHandler implements Runnable {

		private final Socket socket;

		private final UUID sessionId;
		private final long timestamp;

		public ClientHandler(Socket socket, long timestamp) {
			this.socket = socket;
			this.sessionId = UUID.randomUUID();
			this.timestamp = timestamp;
		}

		@Override
		public void run() {
			try {
				// Get input and output streams for the socket
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(),
						true);

				// Parse the client request and ensure it is a valid request
				String line = in.readLine();
				String[] httpRequest = line != null ? line.split(" ") : null;
				if (httpRequest == null
						|| httpRequest.length != 3
						|| !httpRequest[0].equals("GET")
						|| (!httpRequest[2].equals("HTTP/1.0") && !httpRequest[2]
								.equals("HTTP/1.1"))) {
					writeEmptyResponse(RESPONSE_STATUS_GATEWAY_INVALID_REQUEST,
							out);
					return;
				}
				Request request = new Request(httpRequest[1]);

				// Validate the user credentials
				String username = request.get(MY_USERNAME);
				String password = request.get(MY_PASSWORD);
				if (!validateUser(username, password)) {
					writeEmptyResponse(
							RESPONSE_STATUS_GATEWAY_AUTHENTICATION_FAILED, out);
					return;
				}

				// Handle all valid paths for the request
				switch (request.getPath()) {

				case GATEWAY_PATH_NEARBY_SEARCH:
					doPlaceSearch(request, API_PATH_NEARBY_SEARCH, out);
					break;

				case GATEWAY_PATH_TEXT_SEARCH:
					doPlaceSearch(request, API_PATH_TEXT_SEARCH, out);
					break;

				case GATEWAY_PATH_RADAR_SEARCH:
					doPlaceSearch(request, API_PATH_RADAR_SEARCH, out);
					break;

				case GATEWAY_PATH_ADD_USER:
					addUser(request, out);
					break;

				case GATEWAY_PATH_SEARCH_QUERY:
					executeQuery(request, SEARCH_QUERY_PREFIX, QUERY_SUFFIX,
							out);
					break;

				case GATEWAY_PATH_RESULT_QUERY:
					executeQuery(request, RESULT_QUERY_PREFIX, QUERY_SUFFIX,
							out);
					break;

				default:
					writeEmptyResponse(RESPONSE_STATUS_GATEWAY_INVALID_URL, out);
					break;
				}

			} catch (IOException e) {
				Log.e("Error reading client request", e);
				return;
			} catch (Exception e) {
				// Catch runtime exceptions
				Log.e("Unknown error handling client request", e);
				return;
			} finally {
				try {
					if (socket != null) {
						socket.close();
					}
				} catch (IOException e) {
					// Do nothing because we are exiting
					Log.e("Exception when closing socket", e);
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
		 * @return True of the search was successfully, false otherwise
		 *
		 * @throws IOException
		 *             if the query could not be completed successfully
		 */
		private boolean doPlaceSearch(Request request, String apiPath,
				PrintWriter out) {
			SSLSocket apiSocket = null;

			try {
				// Open a connection to the API server
				apiSocket = (SSLSocket) SSLSocketFactory.getDefault()
						.createSocket(API_HOSTNAME, 443);

				// Send the API HTTP request
				PrintWriter apiOut = new PrintWriter(new OutputStreamWriter(
						apiSocket.getOutputStream()), true);
				apiOut.println("GET " + apiPath + request.getQuery()
						+ " HTTP/1.0");
				apiOut.println();

				// Read and forward the response
				BufferedReader in = new BufferedReader(new InputStreamReader(
						apiSocket.getInputStream()));
				StringBuilder response = new StringBuilder();
				String line;

				// Read and forward the HTTP response headers
				while ((line = in.readLine()) != null) {
					out.println(line);
					if (line.isEmpty()) {
						break;
					}
				}

				// Read and forward the HTTP response body
				while ((line = in.readLine()) != null) {
					out.println(line);
					response.append(line);
				}
				JSONObject jsonResponse = new JSONObject(response.toString());

				// Write the search and the results to the database
				if (dbUrl != null
						&& jsonResponse.getString(RESPONSE_STATUS).equals(
								RESPONSE_STATUS_OK)) {
					writeSearch(request);
					writeResults(request.get(MY_USERNAME),
							jsonResponse.getJSONArray(RESPONSE_RESULTS));
				}

			} catch (IOException e) {
				Log.e("Error performing search using Google Places Search API",
						e);
				writeEmptyResponse(RESPONSE_STATUS_SEARCH_ERROR, out);
				return false;
			} catch (Exception e) {
				// Catch runtime exceptions
				Log.e("Unknown error performing search using Google Places Search API",
						e);
				writeEmptyResponse(RESPONSE_STATUS_GATEWAY_UNKNOWN_ERROR, out);
				return false;
			} finally {
				try {
					if (apiSocket != null) {
						apiSocket.close();
					}
				} catch (IOException e) {
					// Do nothing because we are exiting
					Log.e("Exception when closing socket", e);
				}
			}

			return true;
		}

		/**
		 * Writes a search to the database.
		 *
		 * @param request
		 *            the performed search
		 *
		 * @return True if the search was written successfully, false otherwise
		 */
		private boolean writeSearch(Request request) {
			Connection connection = null;
			PreparedStatement statement = null;

			try {
				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the update
				statement = connection.prepareStatement(INSERT_SEARCH);
				statement.setObject(1, sessionId);
				statement.setTimestamp(2, new Timestamp(timestamp));
				statement.setString(3, request.get(MY_USERNAME));
				statement.setString(4, request.get(QUERY));
				statement.setString(5, request.get(LOCATION));
				statement.setString(6, request.get(RADIUS));
				statement.setString(7, request.get(KEYWORD));
				statement.setString(8, request.get(LANGUAGE));
				statement.setString(9, request.get(MINPRICE));
				statement.setString(10, request.get(MAXPRICE));
				statement.setString(11, request.get(NAME));
				statement.setString(12, request.get(OPENNOW));
				statement.setString(13, request.get(RANKBY));
				statement.setString(14, request.get(TYPES));
				statement.setString(15, request.get(PAGETOKEN));
				statement.setString(16, request.get(ZAGATSELECTED));
				statement.executeUpdate();

			} catch (SQLException e) {
				// Do nothing and do not write to database
				Log.e("Error writing search to database", e);
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
					Log.e("Exception when closing database resources", e);
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
		private boolean writeResults(String username, JSONArray results) {
			Connection connection = null;
			PreparedStatement statement = null;

			try {
				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the updates
				statement = connection.prepareStatement(INSERT_RESULT);
				for (int i = 0; i < results.length(); i++) {
					JSONObject result = results.getJSONObject(i);
					statement.setObject(1, sessionId);
					statement.setTimestamp(2, new Timestamp(timestamp));
					statement.setString(3, username);
					statement.setString(4, result.getString("place_id"));
					statement.setDouble(5, result.getJSONObject("geometry")
							.getJSONObject("location").getDouble("lat"));
					statement.setDouble(6, result.getJSONObject("geometry")
							.getJSONObject("location").getDouble("lng"));
					statement.executeUpdate();
				}

			} catch (SQLException e) {
				// Do nothing and do not write to database
				Log.e("Error writing results to database", e);
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
					Log.e("Exception when closing database resources", e);
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
		 * @return True if the query was successfully added, false otherwise
		 */
		// TODO check javadoc
		private boolean executeQuery(Request request, String queryPrefix,
				String querySuffix, PrintWriter out) {
			Query query = new Query(request, queryPrefix, querySuffix);

			Connection connection = null;
			PreparedStatement statement = null;
			ResultSet resultSet = null;

			try {
				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the query
				statement = connection.prepareStatement(query.getQuery());
				for (int i = 0; i < query.size(); i++) {
					statement.setString(i + 1, query.get(i));
				}
				resultSet = statement.executeQuery();

				// Write the response to the client
				writeQueryResponse(resultSet, out);

			} catch (SQLException e) {
				Log.e("Error executing database query", e);
				writeEmptyResponse(RESPONSE_STATUS_GATEWAY_QUERY_ERROR, out);
				return false;
			} catch (Exception e) {
				// Catch runtime exceptions
				Log.e("Unknown error executing database query", e);
				writeEmptyResponse(RESPONSE_STATUS_GATEWAY_UNKNOWN_ERROR, out);
				return false;
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
					Log.e("Exception when closing database resources", e);
				}
			}

			return true;
		}

		/**
		 * Adds a user with the username and password specified in the request
		 * as an authorized user.
		 *
		 * @param request
		 *            the request containing the username and password
		 *
		 * @return True if the user was successfully added, false otherwise
		 */
		private boolean addUser(Request request, PrintWriter out) {
			Connection connection = null;
			PreparedStatement statement = null;

			try {
				// Get a new salt and hash the password
				byte[] salt = getSalt();
				Log.i(request.getParameters().keySet().toString());
				byte[] hashedPassword = hashPassword(request.get(NEW_PASSWORD),
						salt);

				// Open a connection to the database
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);

				// Execute the update to add the user
				statement = connection.prepareStatement(ADD_USER);
				statement.setString(1, request.get(NEW_USERNAME));
				statement.setString(2, encode(salt));
				statement.setString(3, encode(hashedPassword));
				statement.executeUpdate();

				// Write the response to the client
				writeEmptyResponse(RESPONSE_STATUS_OK, out);

			} catch (SQLException e) {
				Log.e("Error adding user to database", e);
				writeEmptyResponse(RESPONSE_STATUS_GATEWAY_ADD_USER_ERROR, out);
				return false;
			} catch (NoSuchAlgorithmException e) {
				Log.e("Attempted to use invalid hash algorithm", e);
				writeEmptyResponse(RESPONSE_STATUS_GATEWAY_ADD_USER_ERROR, out);
				return false;
			} catch (Exception e) {
				// Catch runtime exceptions
				Log.e("Unknown error adding user to database", e);
				writeEmptyResponse(RESPONSE_STATUS_GATEWAY_UNKNOWN_ERROR, out);
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
					Log.e("Exception when closing database resources", e);
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
			// Return false if anonymous users are not permitted
			if (username == null) {
				return allowAnonUsers();
			}

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

			} catch (SQLException e) {
				Log.e("Error validating user credentials with database", e);
				return false;
			} catch (NoSuchAlgorithmException e) {
				Log.e("Attempted to use invalid hash algorithm", e);
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
					Log.e("Exception when closing database resources", e);
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
	private static void writeQueryResponse(ResultSet resultSet, PrintWriter out) {
		try {
			JSONObject jsonResponse = new JSONObject();
			JSONArray jsonResults = new JSONArray();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

			while (resultSet.next()) {
				JSONObject jsonResult = new JSONObject();
				int cols = resultSetMetaData.getColumnCount();
				for (int i = 1; i <= cols; i++) {
					jsonResult.put(resultSetMetaData.getColumnLabel(i)
							.toLowerCase(), resultSet.getObject(i));
				}

				jsonResults.put(jsonResult);
			}

			jsonResponse.put(RESPONSE_STATUS, RESPONSE_STATUS_OK);
			jsonResponse.put(RESPONSE_RESULTS, jsonResults);

			out.println(jsonResponse.toString(3));

		} catch (SQLException e) {
			Log.e("Error reading database query results", e);
			writeEmptyResponse(RESPONSE_STATUS_GATEWAY_QUERY_ERROR, out);
		}
	}

	/**
	 * Returns a {@link JSONObject} with empty results and the specified status.
	 *
	 * @param status
	 *            the status
	 *
	 * @return A {@link JSONObject} with empty results and the specified status
	 */
	private static void writeEmptyResponse(String status, PrintWriter out) {
		JSONObject jsonResponse = new JSONObject();

		jsonResponse.put(RESPONSE_STATUS, status);
		jsonResponse.put(RESPONSE_RESULTS, new JSONArray());

		out.println(jsonResponse.toString(3));
	}

	/**
	 * Returns whether anonymous users are allowed.
	 *
	 * @return True if anonymous users are allowed, false otherwise
	 */
	private static boolean allowAnonUsers() {
		return ALLOW_ANON_USERS;
	}

	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();
		String dbUrl = null;
		String dbUsername = null;
		String dbPassword = null;

		try {
			// Read database information from properties file
			properties.load(new FileInputStream(PROPERTIES));
			dbUrl = properties.getProperty(DB_URL);
			dbUsername = properties.getProperty(DB_USERNAME);
			dbPassword = properties.getProperty(DB_PASSWORD);

		} catch (IOException e) {
			// Do nothing and start gateway without database
			Log.e(String.format(
					"Error reading database connection information from %s",
					PROPERTIES), e);
		}

		// Start gateway server
		Gateway server = new Gateway(8080, dbUrl, dbUsername, dbPassword);
		server.start();
	}
}