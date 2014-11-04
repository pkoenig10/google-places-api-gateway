package places;

import org.json.JSONObject;

/**
 * A result from a Google Place Search API query.
 */
public class PlaceResult {

	private final String placeId;
	private final double latitude;
	private final double longitude;

	public PlaceResult(JSONObject result) {
		placeId = parsePlaceId(result);
		latitude = parseLatitude(result);
		longitude = parseLongitude(result);
	}

	/**
	 * Get the place ID of this result.
	 *
	 * @return The place ID of this result
	 */
	public String getPlaceId() {
		return placeId;
	}

	/**
	 * Get the latitude of this result.
	 *
	 * @return The latitude of this result
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Get the longitude of this result.
	 *
	 * @return The longitude of this result
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Parses the place ID from a {@link JSONObject}.
	 *
	 * @return The place ID of this result
	 */
	private static String parsePlaceId(JSONObject result) {
		return result.getString("place_id");
	}

	/**
	 * Parses the latitude from a {@link JSONObject}.
	 *
	 * @return The latitude of this result
	 */
	private static double parseLatitude(JSONObject result) {
		return result.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lat");
	}

	/**
	 * Parses the longitude from a {@link JSONObject}.
	 *
	 * @return The longitude of this result
	 */
	private static double parseLongitude(JSONObject result) {
		return result.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lng");
	}
}
