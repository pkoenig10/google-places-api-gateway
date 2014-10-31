package places;

/**
 * Parameters for a Google Places API nearby search. See
 * https://developers.google.com/places/documentation/search for more
 * information.
 */
public interface PlaceParameters {

	public static final String QUERY = "query";
	public static final String KEY = "key";
	public static final String LOCATION = "location";
	public static final String RADIUS = "radius";
	public static final String KEYWORD = "keyword";
	public static final String LANGUAGE = "language";
	public static final String MINPRICE = "minprice";
	public static final String MAXPRICE = "maxprice";
	public static final String NAME = "name";
	public static final String OPENNOW = "opennow";
	public static final String RANKBY = "rankby";
	public static final String TYPES = "types";
	public static final String PAGETOKEN = "pagetoken";
	public static final String ZAGATSELECTED = "zagatselected";
}
