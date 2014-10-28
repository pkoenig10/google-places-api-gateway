package places.nearbySearch;

/**
 * Parameters for a Google Places API nearby search. See
 * https://developers.google.com/places/documentation/search for more
 * information.
 */
public interface NearbySearchParameters {

	// Required parameters
	public static final String key = "key";
	public static final String location = "location";
	public static final String radius = "radius";

	// Optional parameters
	public static final String keyword = "keyword";
	public static final String language = "language";
	public static final String minprice = "minprice";
	public static final String maxprice = "maxprice";
	public static final String name = "name";
	public static final String opennow = "opennow";
	public static final String rankby = "rankby";
	public static final String types = "types";
	public static final String pagetoken = "pagetoken";
	public static final String zagatselected = "zagatselected";
}
