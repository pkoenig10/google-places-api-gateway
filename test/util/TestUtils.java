package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class TestUtils {

	private static final String CLEAR_SEARCHES = "DELETE FROM searches;";
	private static final String CLEAR_RESULTS = "DELETE FROM results;";
	private static final String CLEAR_USERS = "DELETE FROM users;";

	public static void doRequest(String spec) throws IOException {
		// Print request and make connection
		System.out.format("Request:%n%s%n%n", spec);
		URL url = new URL(spec);
		URLConnection connection = url.openConnection();

		// Get and print response
		System.out.println("Response:");
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		String line;
		while ((line = in.readLine()) != null)
			System.out.println(line);
		in.close();
	}

	public static void doSilentRequest(String spec) throws IOException {
		// Make the request
		URL url = new URL(spec);
		URLConnection connection = url.openConnection();
		connection.getInputStream();
	}

	public static void clearDatabase(String url) throws SQLException {
		Connection connection = null;
		Statement statement = null;

		try {
			// Open a connection to the database
			connection = DriverManager.getConnection(url);

			// Execute the queries
			statement = connection.createStatement();
			statement.executeUpdate(CLEAR_SEARCHES);
			statement.executeUpdate(CLEAR_RESULTS);
			statement.executeUpdate(CLEAR_USERS);

		} catch (SQLException e) {
			// Rethrow the exception
			throw e;
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
	}
}
