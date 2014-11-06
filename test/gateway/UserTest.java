package gateway;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.TestUtils;

public class UserTest {

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
	}

	@After
	public void tearDown() throws Exception {
		System.out.format("%n%n%n");
		gateway.shutdown();
		// TestUtils.clearDatabase(URL);
	}

	@Test
	public void invalidUser() throws Exception {
		System.out.format("Invalid user%n%n");
		TestUtils
				.doRequest(String
						.format("http://localhost:%d/google-places-api-gateway/nearbysearch?username=invalid&password=invalid&location=-33.8670522,151.1957362&radius=500&types=food&name=cruise&key=%s",
								PORT, API_KEY));
	}

	@Test
	public void addUser() throws Exception {
		System.out.format("Add user%n%n");
		TestUtils
				.doRequest(String
						.format("http://localhost:%d/google-places-api-gateway/adduser?newusername=pkoenig&newpassword=wordpass",
								PORT));
	}

	@Test
	public void validUser() throws Exception {
		System.out.format("Valid user%n%n");
		TestUtils
				.doSilentRequest(String
						.format("http://localhost:%d/google-places-api-gateway/adduser?newusername=pkoenig&newpassword=wordpass",
								PORT));

		// Add the user and wait for the gateway to finish handling the request
		// and write to the database
		Thread.sleep(2000);

		TestUtils
				.doRequest(String
						.format("http://localhost:%d/google-places-api-gateway/nearbysearch?username=pkoenig&password=wordpass&location=-33.8670522,151.1957362&radius=500&types=food&name=cruise&key=%s",
								PORT, API_KEY));
	}

}
