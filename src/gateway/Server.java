package gateway;

import static gateway.RequestUrls.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
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

import org.json.JSONArray;
import org.json.JSONObject;

import places.PlaceResult;
import places.PlaceSearch;

public class Server extends Thread {

	private static final String INSERT_RESULT = "INSERT INTO results (session_id, username, place_id, lat, lng) VALUES (?, ?, ?, ?, ?);";

	private static final int NUM_THREADS = 10;

	private final int port;

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
			e.printStackTrace();
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
				e.printStackTrace();
				break;
			}
		}

		try {
			serverSocket.close();
		} catch (IOException e) {
			// Ignore because we're about to exit anyway.
		} finally {
			executor.shutdown();
		}
	}

	class ClientHandler implements Runnable {

		private final Socket socket;

		private final UUID sessionId;

		public ClientHandler(Socket socket) {
			this.socket = socket;
			this.sessionId = UUID.randomUUID();
		}

		@Override
		public void run() {
			// TODO close io streams
			try {

				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				String[] httpRequest = in.readLine().split(" ");
				if (httpRequest.length != 3 || !httpRequest[0].equals("GET")) {
					// TODO handle error
					System.out.println(httpRequest.length != 3);
					System.out.println(!httpRequest[0].equals("GET"));
					System.out.println(!httpRequest[2].equals("HTTP/1.0"));
					System.out.println(!httpRequest[2].equals("HTTP/1.1"));
					return;
				}

				PlaceSearch search = getSearch(httpRequest[1]);

				PrintWriter out = new PrintWriter(socket.getOutputStream(),
						true);
				JSONObject response = search.doSearch(out);

				if (response.getString("status").equals("OK")) {
					writeSearch(search);
					writeResults(response.getJSONArray("results"));
				}
			} catch (IOException e) {
				e.printStackTrace();
				return;
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					// Ignore because we're about to exit anyway.
				}
			}
		}

		private PlaceSearch getSearch(String path) throws MalformedURLException {
			URL url = new URL("http", "localhost", path);

			switch (url.getPath()) {
			case NEARBY_SEARCH_PATH:
				return new PlaceSearch(NEARBY_SEARCH_API_URL, url.getQuery());
			case TEXT_SEARCH_PATH:
				return new PlaceSearch(TEXT_SEARCH_API_URL, url.getQuery());
			case RADAR_SEARCH_PATH:
				return new PlaceSearch(RADAR_SEARCH_API_URL, url.getQuery());
			default:
				throw new MalformedURLException("invalid request: " + path);
			}
		}

		private void writeSearch(PlaceSearch search) {

		}

		private void writeResults(JSONArray results) {
			Connection connection = null;
			PreparedStatement statement = null;
			try {
				// Open a connection
				connection = DriverManager.getConnection(dbUrl, dbUser,
						dbPassword);
				statement = connection.prepareStatement(INSERT_RESULT);

				// Execute the updates
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
				e.printStackTrace();
			} finally {
				try {
					if (statement != null) {
						statement.close();
					}
					if (connection != null) {
						connection.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		Server server = new Server(12345);
		server.start();
	}
}
