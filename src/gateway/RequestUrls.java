package gateway;

/**
 * HTTP URLs for Google Places API requests. See
 * https://developers.google.com/places/documentation/search for more
 * information.
 */
public interface RequestUrls {

	public static final String nearbySearchURL = "https://maps.googleapis.com/maps/api/place/nearbysearch/";
	public static final String textSearchURL = "https://maps.googleapis.com/maps/api/place/textsearch/";
	public static final String radarSearchURL = "https://maps.googleapis.com/maps/api/place/radarsearch/";
}
