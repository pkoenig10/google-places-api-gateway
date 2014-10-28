package places.textSearch;

/**
 * Parameters for a Google Places API text search. See
 * https://developers.google.com/places/documentation/search for more
 * information.
 */
public class TextSeachParameters {

	// Required parameters
	public static final String QUERY = "query";
	public static final String KEY = "key";

	// Optional parameters
	public static final String LOCATION = "location";
	public static final String RADIUS = "radius";
	public static final String LANGUAGE = "language";
	public static final String MINPRICE = "minprice";
	public static final String MAXPRICE = "maxprice";
	public static final String OPENNOW = "opennow";
	public static final String TYPES = "types";
	public static final String ZAGATSELECTED = "zagatselected";
}
