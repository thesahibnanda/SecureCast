#include <crow.h>
#include "handlers.h"
#include <spdlog/spdlog.h>

crow::response handle_add_user(const crow::request& req, ThreadSafeQueue<std::string>& userQueue) {
    spdlog::info("Received request to add user.");

    auto json_req = crow::json::load(req.body);

    if (!json_req || !json_req.has("data") || !json_req["data"].s().size()) {
        spdlog::error("Invalid request payload. 'data' field is missing or not a string.");
        return crow::response(400, "{\"error\":true, \"is_added\":false}");
    }

    std::string userData = json_req["data"].s();

    bool is_added = userQueue.enqueue(userData);
    if (!is_added) {
        spdlog::warn("Queue is full. Unable to add more users.");
        return crow::response(429, "{\"error\":true, \"is_added\":false}");
    }

    spdlog::info("User added to the queue successfully.");
    crow::json::wvalue res;
    res["error"] = false;
    res["is_added"] = true;
    return crow::response(201, res);
}

crow::response handle_get_user(ThreadSafeQueue<std::string>& userQueue) {
    spdlog::info("Received request to fetch next user.");

    std::string userData;
    crow::json::wvalue res;

    if (userQueue.dequeue(userData)) {
        spdlog::info("User fetched from queue successfully.");
        res["error"] = false;
        res["data"] = userData;
        return crow::response(200, res);
    } else {
        spdlog::info("Queue is empty. No users to fetch.");
        res["error"] = false;
        res["data"] = nullptr;
        return crow::response(204, res);
    }
}

crow::response handle_health_check() {
    spdlog::info("Health check requested.");
    crow::json::wvalue res;
    res["status"] = "healthy";
    return crow::response(200, res);
}

crow::response handle_metrics(ThreadSafeQueue<std::string>& userQueue) {
    spdlog::info("Metrics requested.");
    crow::json::wvalue res;
    res["queue_size"] = userQueue.size();
    return crow::response(200, res);
}
