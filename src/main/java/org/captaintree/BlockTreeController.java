package org.captaintree;

import com.google.gson.Gson;
import org.captaintree.blocktree.BlockTree;
import org.captaintree.pojo.Block;
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
        path("/api", () -> {
            post("/user/add", (req, res) -> addUser(req.body(), res));
            put("/user/update", (req, res) -> updateUser(req.body(), res));
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

    private static String jsonResponse(Response res, boolean error, String message, Map<String, Object> data) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("error", error);
        responseMap.put("message", message);
        if (data != null) responseMap.putAll(data);
        res.type("application/json");
        return gson.toJson(responseMap);
    }
}
