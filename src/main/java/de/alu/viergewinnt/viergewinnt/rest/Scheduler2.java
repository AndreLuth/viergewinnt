package de.alu.viergewinnt.viergewinnt.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// For local game playing use two players
//@Singleton
//@Startup
public class Scheduler2 { // player 2

	private static String ENDPOINT = "http://connect-4-api-dev7-connect4.apps.cluster-sva-7909.sva-7909.example.opentlc.com/";

	private static final String PLAYER = "player_2";

	private static final String BOARD_ENDPOINT = ENDPOINT + "board";

	private static final String TURN_ENDPOINT = ENDPOINT + "turn";

//	@Schedule(hour = "*", minute = "*", second = "*")
	public void excute() {

		StringBuilder jsonBuilder = new StringBuilder();
		try {

			URL url = new URL(BOARD_ENDPOINT);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			try (Reader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
					Charset.forName(StandardCharsets.UTF_8.name())))) {
				int c = 0;
				while ((c = reader.read()) != -1) {
					jsonBuilder.append((char) c);
				}
			}

			conn.disconnect();

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		handleResponse(jsonBuilder.toString());

	}

	private void handleResponse(String response) {

		if (response == null) {
			System.out.println("Response is null.");
			return;
		}

		JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();

		if (!jsonObject.isJsonObject()) {
			System.out.println("No JSON return.");
			return;
		}

		String status = jsonObject.get("status").getAsString();
		if ("finished".equals(status)) {
			System.out.println("Game finished.");
			return;
		}

		String turn = jsonObject.get("turn").getAsString();
		if (!PLAYER.equals(turn)) {
			System.out.println("Another player is on turn: " + turn);
			return;
		}

		JsonArray jsonArray = jsonObject.get("field").getAsJsonArray();
		int[] field = new Gson().fromJson(jsonArray, int[].class);

		play(field);
	}

	private void play(int[] field) {
		System.out.println("Play");

		// Field:
//		 0  1  2  3  4  5  6
//		 7  8  9 10 11 12 13
//		14 15 16 17 ...
//		... 41

		// TODO play: maybe search the last zero and use this -> the last one is always on the last line
		if (field != null && field.length > 0) {
			for (int i = field.length - 1; i >= 0; i--) {
				if (field[i] == 0) {
					System.out.println("Turn on field: " + i);
					makeTurn(i);
					return;
				}
			}
		}

		System.out.println("No valid turn found.");

	}

	private void makeTurn(int position) {
		try {

			URL url = new URL(TURN_ENDPOINT);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("Accept", "application/json");
			conn.setDoOutput(true);

			String turnJson = "{\"turn\":\"" + PLAYER + "\",\"position\":" + position + "}";

			System.out.println("To be send: " + turnJson);

			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = turnJson.getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			StringBuilder jsonBuilder = new StringBuilder();
			try (Reader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
					Charset.forName(StandardCharsets.UTF_8.name())))) {
				int c = 0;
				while ((c = reader.read()) != -1) {
					jsonBuilder.append((char) c);
				}
			}

			conn.disconnect();

			String response = jsonBuilder.toString();

			System.out.println("Turn response: " + response);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
