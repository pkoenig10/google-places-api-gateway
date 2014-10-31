package places;

import org.json.JSONObject;

public class PlaceResult {

	private final String placeId;
	private final double latitude;
	private final double longitude;

	public PlaceResult(JSONObject result) {
		placeId = parsePlaceId(result);
		latitude = parseLatitude(result);
		longitude = parseLongitude(result);
	}

	public String getPlaceId() {
		return placeId;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	private static String parsePlaceId(JSONObject result) {
		return result.getString("place_id");
	}

	private static double parseLatitude(JSONObject result) {
		return result.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lat");
	}

	private static double parseLongitude(JSONObject result) {
		return result.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lng");
	}
}
