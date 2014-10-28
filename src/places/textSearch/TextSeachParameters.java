package places.textSearch;

/**
 * Parameters for a Google Places API text search. See
 * https://developers.google.com/places/documentation/search for more
 * information.
 */
public class TextSeachParameters {

	// Required parameters
	public static final String query = "query";
	public static final String key = "key";

	// Optional parameters
	public static final String location = "location";
	public static final String radius = "radius";
	public static final String language = "language";
	public static final String minprice = "minprice";
	public static final String maxprice = "maxprice";
	public static final String opennow = "opennow";
	public static final String types = "types";
	public static final String zagatselected = "zagatselected";
}
