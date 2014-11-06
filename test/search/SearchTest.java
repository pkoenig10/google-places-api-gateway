package search;

import gateway.Gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
		doRequest(String
				.format("http://localhost:%d/google-places-api-gateway/nearbysearch?location=-33.8670522,151.1957362&radius=500&types=food&name=cruise&key=%s",
						PORT, API_KEY));
	}

	@Test
	public void textSearch() throws Exception {
		System.out.format("Text Search%n%n");

		// Example from
		// https://developers.google.com/places/documentation/search
		doRequest(String
				.format("http://localhost:%d/google-places-api-gateway/textsearch?query=restaurants+in+Sydney&key=%s",
						PORT, API_KEY));
	}

	@Test
	public void radarSearch() throws Exception {
		System.out.format("Radar Search%n%n");

		// Example from
		// https://developers.google.com/places/documentation/search
		doRequest(String
				.format("http://localhost:%d/google-places-api-gateway/radarsearch?location=51.503186,-0.126446&radius=500&types=museum&key=%s",
						PORT, API_KEY));
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

}
