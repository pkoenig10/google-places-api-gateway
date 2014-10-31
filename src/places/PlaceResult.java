package places;

import org.json.JSONObject;

public class PlaceResult {

	private final String placeId;
	private final int latitude;
	private final int longitude;

	public PlaceResult(JSONObject result) {
		placeId = parsePlaceId(result);
		latitude = parseLatitude(result);
		longitude = parseLongitude(result);
	}

	public String getPlaceId() {
		return placeId;
	}

	public int getLatitude() {
		return latitude;
	}

	public int getLongitude() {
		return longitude;
	}

	private static String parsePlaceId(JSONObject result) {
		return result.getString("place_id");
	}

	private static int parseLatitude(JSONObject result) {
		return result.getJSONObject("geometry").getJSONObject("location")
				.getInt("lat");
	}

	private static int parseLongitude(JSONObject result) {
		return result.getJSONObject("geometry").getJSONObject("location")
				.getInt("lng");
	}
}
