package query;

import gateway.Gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueryTest {

	private static final String CLEAR_SEARCHES = "DELETE FROM searches";
	private static final String CLEAR_RESULTS = "DELETE FROM results";
	private static final String CLEAR_USERS = "DELETE FROM users";

	// Add your API key here
	private static final String API_KEY = "AIzaSyBLaiK-G7ibmfmQ9vhtQBIGfjqdM7LaDlk";

	private static final int PORT = 8080;
	private static final String URL = "jdbc:sqlite:db/googleplaces.db";

	private Gateway gateway;

	@Before
	public void setUp() throws Exception {
		clearDatabase();
		gateway = new Gateway(PORT, URL, null);
		gateway.start();
		doSeaches();
	}

	@After
	public void tearDown() throws Exception {
		System.out.format("%n%n%n");
		gateway.shutdown();
		clearDatabase();
	}

	@Test
	public void searchQuery() throws Exception {
		System.out.format("Search Query%n%n");
		doRequest(String.format(
				"http://localhost:%d/google-places-api-gateway/searchquery",
				PORT));
	}

	@Test
	public void resultQuery() throws Exception {
		System.out.format("Result Query%n%n");
		doRequest(String.format(
				"http://localhost:%d/google-places-api-gateway/resultquery",
				PORT));
	}

	private static void doRequest(String spec) throws IOException {
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

	private static void clearDatabase() throws SQLException {
		Connection connection = null;
		Statement statement = null;

		try {
			// Open a connection to the database
			connection = DriverManager.getConnection(URL);

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

	private static void doSeaches() throws IOException, InterruptedException {
		// Perform searches so there is data in the database. Searches are from
		// https://developers.google.com/places/documentation/search
		doSearch(String
				.format("http://localhost:%d/google-places-api-gateway/nearbysearch?location=-33.8670522,151.1957362&radius=500&types=food&name=cruise&key=%s",
						PORT, API_KEY));
		doSearch(String
				.format("http://localhost:%d/google-places-api-gateway/textsearch?query=restaurants+in+Sydney&key=%s",
						PORT, API_KEY));
		doSearch(String
				.format("http://localhost:%d/google-places-api-gateway/radarsearch?location=51.503186,-0.126446&radius=500&types=museum&key=%s",
						PORT, API_KEY));

		// Wait for the gateway to finish handling the search requests and write
		// to the database
		Thread.sleep(2000);
	}

	private static void doSearch(String spec) throws IOException {
		// Make the request
		URL url = new URL(spec);
		URLConnection connection = url.openConnection();
		connection.getInputStream();
	}
}
