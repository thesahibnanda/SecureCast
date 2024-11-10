package org.captaintree;

import com.google.gson.Gson;
import org.captaintree.blocktree.BlockTree;
import org.captaintree.pojo.Block;
import org.captaintree.pojo.User;
import spark.Response;
import spark.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static spark.Spark.*;

public class BlockTreeController {
    private static final BlockTree blockTree = new BlockTree(User.getGenesisUser());
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        port(4567);
        path("", () -> {
            post("/user/add", (req, res) -> addUser(req.body(), res));
            put("/user/update", (req, res) -> updateUser(req.body(), res));
            post("/user/vote", BlockTreeController::voteForParty);
            get("/party/votes", BlockTreeController::getPartyVotes);
            get("/user/vote", BlockTreeController::checkUserVote);
            get("/user/details", BlockTreeController::getUserDetails);
            get("/tree/verify", (req, res) -> verifyTree(res));
            get("/healthz", (req, res) -> healthCheck(res));
            get("/metrics", (req, res) -> getMetrics(res));
            post("/shutdown", (req, res) -> shutdownBlockTree(res));
        });
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

    private static String voteForParty(Request req, Response res) {
        String email = req.queryParams("email");
        String partyName = req.queryParams("party");
        if (email == null || partyName == null) {
            return jsonResponse(res, true, "Email and party name must be provided", null);
        }
        String message = blockTree.voteForParty(email, partyName);
        boolean error = message.contains("already voted") || message.contains("not registered");
        return jsonResponse(res, error, message, null);
    }

    private static String getPartyVotes(Request req, Response res) {
        String partyName = req.queryParams("party");
        if (partyName == null) {
            return jsonResponse(res, true, "Party name must be provided", null);
        }
        int votes = blockTree.getPartyVoteCount(partyName);
        return jsonResponse(res, false, "Vote count retrieved", Map.of("party", partyName, "votes", votes));
    }

    private static String checkUserVote(Request req, Response res) {
        String email = req.queryParams("email");
        if (email == null) {
            return jsonResponse(res, true, "Email must be provided", null);
        }
        Map<String, Object> voteInfo = blockTree.checkUserVote(email);
        return gson.toJson(voteInfo);
    }

    private static String getUserDetails(Request req, Response res) {
        String identifier = req.queryParams("identifier"); // Can be email or Aadhaar
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
