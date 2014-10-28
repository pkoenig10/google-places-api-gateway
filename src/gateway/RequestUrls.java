package gateway;

/**
 * HTTP URLs for Google Places API requests. See
 * https://developers.google.com/places/documentation/search for more
 * information.
 */
public interface RequestUrls {

	// URL paths used by the client to interact with the gateway
	public static final String NEARBY_SEARCH_PATH = "/google-places-api-gateway/nearbysearch";
	public static final String TEXT_SEARCH_PATH = "/google-places-api-gateway/textsearch";
	public static final String RADAR_SEARCH_PATH = "/google-places-api-gateway/radarsearch";

	// URLs used by the gateway to interact with the API
	public static final String NEARBY_SEARCH_API_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
	public static final String TEXT_SEARCH_API_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json?";
	public static final String RADAR_SEARCH_API_URL = "https://maps.googleapis.com/maps/api/place/radarsearch/json?";
}
