package places.radarSearch;

/**
 * Parameters for a Google Places API radar search. See
 * https://developers.google.com/places/documentation/search for more
 * information.
 */
public interface RadarSearchParameters {

	// Required parameters
	public static final String KEY = "key";
	public static final String LOCATION = "location";
	public static final String RADIUS = "radius";

	// Optional parameters
	public static final String KEYWORD = "keyword";
	public static final String MINPRICE = "minprice";
	public static final String MAXPRICE = "maxprice";
	public static final String NAME = "name";
	public static final String OPENNOW = "opennow";
	public static final String TYPES = "types";
	public static final String ZAGATSELECTED = "zagatselected";
}
