package gateway;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.TestUtils;

public class SearchTest {

	// Add your API key here
	private static final String API_KEY = "AIzaSyBLaiK-G7ibmfmQ9vhtQBIGfjqdM7LaDlk";

	private static final int PORT = 8080;

	private Gateway gateway;

	@Before
	public void setUp() {
		gateway = new Gateway(PORT, null, null, null, null);
		gateway.start();
	}

	@After
	public void tearDown() {
		System.out.format("%n%n%n");
		gateway.shutdown();
	}

	@Test
	public void nearbySearch() throws Exception {
		System.out.format("Nearby Search%n%n");

		// Example from
		// https://developers.google.com/places/documentation/search
		TestUtils
				.doRequest(String
						.format("http://localhost:%d/google-places-api-gateway/nearbysearch?location=-33.8670522,151.1957362&radius=500&types=food&name=cruise&key=%s",
								PORT, API_KEY));
	}

	@Test
	public void textSearch() throws Exception {
		System.out.format("Text Search%n%n");

		// Example from
		// https://developers.google.com/places/documentation/search
		TestUtils
				.doRequest(String
						.format("http://localhost:%d/google-places-api-gateway/textsearch?query=restaurants+in+Sydney&key=%s",
								PORT, API_KEY));
	}

	@Test
	public void radarSearch() throws Exception {
		System.out.format("Radar Search%n%n");

		// Example from
		// https://developers.google.com/places/documentation/search
		TestUtils
				.doRequest(String
						.format("http://localhost:%d/google-places-api-gateway/radarsearch?location=51.503186,-0.126446&radius=500&types=museum&key=%s",
								PORT, API_KEY));
	}
}
