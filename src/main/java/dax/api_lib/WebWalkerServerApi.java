package dax.api_lib;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dax.api_lib.json.Json;
import dax.api_lib.json.JsonValue;
import dax.api_lib.json.ParseException;
import dax.api_lib.models.*;
import dax.api_lib.utils.IOHelper;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebWalkerServerApi {

    private static WebWalkerServerApi webWalkerServerApi;
    private static Gson gson = new Gson();

    public static WebWalkerServerApi getInstance() {
        return webWalkerServerApi != null ? webWalkerServerApi : (webWalkerServerApi = new WebWalkerServerApi());
    }

    private static final String WALKER_ENDPOINT = "https://api.dax.cloud";

    private DaxCredentialsProvider daxCredentialsProvider;
    private HashMap<String, String> cache;
    private boolean isTestMode = false;

    private WebWalkerServerApi() {
        cache = new HashMap<>();
    }

    public void setDaxCredentialsProvider(DaxCredentialsProvider daxCredentialsProvider) {
        this.daxCredentialsProvider = daxCredentialsProvider;
    }

    public List<PathResult> getPaths(BulkPathRequest bulkPathRequest) {
        try {
            return parseResults(post(gson.toJson(bulkPathRequest), WALKER_ENDPOINT + "/walker/generatePaths"));
        } catch(IOException e){
            return Collections.singletonList(new PathResult(PathStatus.NO_RESPONSE_FROM_SERVER));
        }
    }

    public PathResult getPath(Point3D start, Point3D end, PlayerDetails playerDetails) {
        if (isTestMode) {
            return getPathX(start, end, playerDetails);
        }

        var reqs = new ArrayList<PathRequestPair>();
        reqs.add(new PathRequestPair(start, end));
        var b = new BulkPathRequest(playerDetails, reqs);
        var res = getPaths(b);
        return res.get(0);
    }

    private PathResult getPathX(Point3D start, Point3D end, PlayerDetails playerDetails) {
        com.google.gson.JsonObject pathRequest = new com.google.gson.JsonObject();
        pathRequest.add("start", start.toJson());
        pathRequest.add("end", end.toJson());

        if (playerDetails != null) {
            pathRequest.add("player", playerDetails.toJson());
        }

        try {
            return parseResult(post(pathRequest, (WALKER_ENDPOINT + "/walker/generatePath")));

        } catch (IOException e) {
            log.info("Is server down? Spam dax.");
            return new PathResult(PathStatus.NO_RESPONSE_FROM_SERVER);
        }
    }

    private List<PathResult> parseResults(ServerResponse serverResponse){
        if (!serverResponse.isSuccess()) {
            JsonValue jsonValue  = null;
            try{
                jsonValue = Json.parse(serverResponse.getContents());
            } catch(Exception | Error e){
                jsonValue = Json.NULL;
            }
            if (!jsonValue.isNull()) {
                log.info("[Error] " + jsonValue.asObject().getString(
                        "message",
                        "Could not generate path: " + serverResponse.getContents()
                                                                             ));
            }

            switch (serverResponse.getCode()) {
                case 429:
                    return Collections.singletonList(new PathResult(PathStatus.RATE_LIMIT_EXCEEDED));
                case 400:
                case 401:
                case 404:
                    return Collections.singletonList(new PathResult(PathStatus.INVALID_CREDENTIALS));
            }
        }

        try {
            return gson.fromJson(serverResponse.getContents(), new TypeToken<List<PathResult>>() {}.getType());
        } catch (ParseException e) {
            PathResult pathResult = new PathResult(PathStatus.UNKNOWN);
            log.error("Response: " + pathResult.getPathStatus());
            return Collections.singletonList(pathResult);
        }
    }

    private PathResult parseResult(ServerResponse serverResponse) {
        if (!serverResponse.isSuccess()) {
            JsonValue jsonValue  = null;
            try{
                jsonValue = Json.parse(serverResponse.getContents());
            } catch(Exception | Error e){
                jsonValue = Json.NULL;
            }
            if (!jsonValue.isNull()) {
                log.info("[Error] " + jsonValue.asObject().getString(
                        "message",
                        "Could not generate path: " + serverResponse.getContents()
                ));
            }

            switch (serverResponse.getCode()) {
                case 429:
                    return new PathResult(PathStatus.RATE_LIMIT_EXCEEDED);
                case 400:
                case 401:
                case 404:
                    return new PathResult(PathStatus.INVALID_CREDENTIALS);
            }
        }

        PathResult pathResult;
        JsonElement jsonObject;
        try {
            jsonObject = new JsonParser().parse(serverResponse.getContents());
        } catch (ParseException e) {
            pathResult = new PathResult(PathStatus.UNKNOWN);
            log.error("Response: " + pathResult.getPathStatus());
            return pathResult;
        }

        pathResult = PathResult.fromJson(jsonObject);
        log.info("Response: " + pathResult.getPathStatus() + " Cost: " + pathResult.getCost());
        return pathResult;
    }

    private ServerResponse post(com.google.gson.JsonObject jsonObject, String endpoint) throws IOException {
        return post(gson.toJson(jsonObject), endpoint);
    }

    private ServerResponse post(String json, String endpoint) throws IOException {
        log.info("Generating path: " + json);
        if (cache.containsKey(json)) {
            log.info("Cached result found: ");
            return new ServerResponse(true, HttpURLConnection.HTTP_OK, cache.get(json.toString()));
        }

        URL myurl = new URL(endpoint);
        HttpURLConnection connection = (HttpsURLConnection) myurl.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);

        connection.setRequestProperty("Method", "POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        IOHelper.appendAuth(connection, daxCredentialsProvider);

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return new ServerResponse(false, connection.getResponseCode(), IOHelper.readInputStream(connection.getErrorStream()));
        }

        String contents = IOHelper.readInputStream(connection.getInputStream());
        log.info("Result from server and caching result: {}", contents);
        cache.put(json, contents);
        return new ServerResponse(true, HttpURLConnection.HTTP_OK, contents);
    }
}
