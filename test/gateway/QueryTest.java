package gateway;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.TestUtils;

public class QueryTest {

	// Add your API key here
	private static final String API_KEY = "AIzaSyBLaiK-G7ibmfmQ9vhtQBIGfjqdM7LaDlk";

	private static final int PORT = 8080;
	private static final String URL = "jdbc:sqlite:db/googleplaces.db";

	private Gateway gateway;

	@Before
	public void setUp() throws Exception {
		TestUtils.clearDatabase(URL);
		gateway = new Gateway(PORT, URL, null, null, null);
		gateway.start();
		doSeaches();
	}

	@After
	public void tearDown() throws Exception {
		System.out.format("%n%n%n");
		gateway.shutdown();
		TestUtils.clearDatabase(URL);
	}

	@Test
	public void searchQuery() throws Exception {
		System.out.format("Search Query%n%n");
		TestUtils.doRequest(String.format(
				"http://localhost:%d/google-places-api-gateway/searchquery",
				PORT));
	}

	@Test
	public void resultQuery() throws Exception {
		System.out.format("Result Query%n%n");
		TestUtils.doRequest(String.format(
				"http://localhost:%d/google-places-api-gateway/resultquery",
				PORT));
	}

	private static void doSeaches() throws IOException, InterruptedException {
		// Perform searches so there is data in the database. Searches are from
		// https://developers.google.com/places/documentation/search
		TestUtils
				.doSilentRequest(String
						.format("http://localhost:%d/google-places-api-gateway/nearbysearch?location=-33.8670522,151.1957362&radius=500&types=food&name=cruise&key=%s",
								PORT, API_KEY));
		TestUtils
				.doSilentRequest(String
						.format("http://localhost:%d/google-places-api-gateway/textsearch?query=restaurants+in+Sydney&key=%s",
								PORT, API_KEY));
		TestUtils
				.doSilentRequest(String
						.format("http://localhost:%d/google-places-api-gateway/radarsearch?location=51.503186,-0.126446&radius=500&types=museum&key=%s",
								PORT, API_KEY));

		// Wait for the gateway to finish handling the search requests and write
		// to the database
		Thread.sleep(2000);
	}
}
