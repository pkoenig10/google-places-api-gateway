package places.radarSearch;

/**
 * Parameters for a Google Places API radar search. See
 * https://developers.google.com/places/documentation/search for more
 * information.
 */
public interface RadarSearchParameters {

	// Required parameters
	public static final String key = "key";
	public static final String location = "location";
	public static final String radius = "radius";

	// Optional parameters
	public static final String keyword = "keyword";
	public static final String minprice = "minprice";
	public static final String maxprice = "maxprice";
	public static final String name = "name";
	public static final String opennow = "opennow";
	public static final String types = "types";
	public static final String zagatselected = "zagatselected";
}
