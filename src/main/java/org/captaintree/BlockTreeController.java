package org.captaintree;

import com.google.gson.Gson;
import org.captaintree.blocktree.BlockTree;
import org.captaintree.pojo.Block;
import org.captaintree.pojo.GetUserRequest;
import org.captaintree.pojo.PartyRequest;
import org.captaintree.pojo.User;
import spark.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static spark.Spark.*;

public class BlockTreeController {
    private static final BlockTree blockTree = new BlockTree(User.getGenesisUser());
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        port(4567);

        enableCORS("*", "*", "*");

        path("", () -> {
            post("/user/add", (req, res) -> addUser(req.body(), res));
            put("/user/update", (req, res) -> updateUser(req.body(), res));
            post("/user/vote", (req, res) -> voteForParty(req.body(), res));
            post("/party/votes", (req, res) -> getPartyVotes(req.body(), res));
            post("/user/check", (req, res) -> checkUserVote(req.body(), res));
            post("/user/details", (req, res) -> getUserDetails(req.body(), res));
            get("/tree/verify", (req, res) -> verifyTree(res));
            get("/tree/get", (req, res) -> getTree(res));
            get("/healthz", (req, res) -> healthCheck(res));
            get("/metrics", (req, res) -> getMetrics(res));
            post("/shutdown", (req, res) -> shutdownBlockTree(res));
        });
    }

    private static void enableCORS(final String origin, final String methods, final String headers) {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Allow-Methods", methods);
            response.header("Access-Control-Allow-Headers", headers);
            response.header("Access-Control-Allow-Credentials", "true");
        });
    }

    private static String getTree(Response response) {
        try {
            return jsonResponse(response, false, "All Tree Data", blockTree.getTreeStructure());
        } catch (Exception e) {
            return jsonResponse(response, true, e.getMessage(), null);
        }
    }

    private static String addUser(String requestBody, Response res) {
        User user = gson.fromJson(requestBody, User.class);
        try {
            Block newBlock = blockTree.addBlockAsync(user).get();
            return jsonResponse(res, false, "User added successfully", Map.of("block", newBlock));
        } catch (ExecutionException | InterruptedException e) {
            return jsonResponse(res, true, "Failed to add user: " + e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return jsonResponse(res, true, e.getMessage(), null);
        }
    }

    private static String updateUser(String requestBody, Response res) {
        User updatedUser = gson.fromJson(requestBody, User.class);
        if (blockTree.updateUser(updatedUser)) {
            return jsonResponse(res, false, "User updated successfully", null);
        } else {
            return jsonResponse(res, true, "User not found for update", null);
        }
    }

    private static String verifyTree(Response res) {
        boolean isValid = blockTree.verifyTreeIntegrity();
        String message = isValid ? "Tree integrity verified successfully" : "Tree integrity check failed";
        return jsonResponse(res, !isValid, message, Map.of("integrity", isValid));
    }

    private static String healthCheck(Response res) {
        return jsonResponse(res, false, "Service is healthy", null);
    }

    private static String getMetrics(Response res) {
        int totalUsers = blockTree.getTotalUsers();
        int totalBlocks = blockTree.getTotalBlocks();
        return jsonResponse(res, false, "Metrics retrieved", Map.of("totalUsers", totalUsers, "totalBlocks", totalBlocks));
    }

    private static String shutdownBlockTree(Response res) {
        blockTree.shutdown();
        return jsonResponse(res, false, "BlockTree shutdown successfully", null);
    }

    private static String voteForParty(String req, Response res) {
        PartyRequest partyRequest = gson.fromJson(req, PartyRequest.class);
        String email = partyRequest.getEmail();
        String partyName = partyRequest.getParty();
        if (email == null || partyName == null) {
            return jsonResponse(res, true, "Email and party name must be provided", null);
        }
        String message = blockTree.voteForParty(email, partyName);
        boolean error = message.contains("already voted") || message.contains("not registered");
        return jsonResponse(res, error, message, null);
    }

    private static String getPartyVotes(String req, Response res) {
        String partyName = gson.fromJson(req, PartyRequest.class).getParty();
        if (partyName == null) {
            return jsonResponse(res, true, "Party name must be provided", null);
        }
        int votes = blockTree.getPartyVoteCount(partyName);
        return jsonResponse(res, false, "Vote count retrieved", Map.of("party", partyName, "votes", votes));
    }

    private static String checkUserVote(String req, Response res) {
        String email = gson.fromJson(req, PartyRequest.class).getEmail();
        if (email == null) {
            return jsonResponse(res, true, "Email must be provided", null);
        }
        Map<String, Object> voteInfo = blockTree.checkUserVote(email);
        if ((boolean) voteInfo.get("error")) {
            return jsonResponse(res, true, (String) voteInfo.get("message"), null);
        }
        return jsonResponse(res, false, String.format("User voted for %s", voteInfo.get("party")), Map.of("party", voteInfo.get("party")));
    }

    private static String getUserDetails(String req, Response res) {
        String identifier = gson.fromJson(req, GetUserRequest.class).getIdentifier();
        if (identifier == null) {
            return jsonResponse(res, true, "Identifier must be provided", null);
        }
        User user = blockTree.getUserByEmailOrAadhar(identifier);
        if (user == null) {
            return jsonResponse(res, true, "User not found", null);
        }
        return jsonResponse(res, false, "User details retrieved", Map.of("user", user));
    }

    private static String jsonResponse(Response res, boolean error, String message, Map<String, Object> data) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", error);
        responseMap.put("message", message);
        if (data != null) responseMap.putAll(data);
        res.type("application/json");
        return gson.toJson(responseMap);
    }
}