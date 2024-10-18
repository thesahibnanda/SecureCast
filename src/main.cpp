#include <crow.h>
#include "ThreadSafeQueue.h"
#include "handlers.h"
#include "signal_handler.h"
#include <boost/asio/thread_pool.hpp>
#include <boost/asio/post.hpp>
#include <spdlog/spdlog.h>
#include <signal.h>

ThreadSafeQueue<std::string> userQueue;
boost::asio::thread_pool pool(12);

int main() {
    spdlog::set_level(spdlog::level::info);
    spdlog::info("Starting InvisibleQueue service...");

    signal(SIGTERM, signal_handler);

    crow::SimpleApp app;

    // Add user route (POST /add-user)
    CROW_ROUTE(app, "/add-user").methods("POST"_method)([](const crow::request& req){
        return handle_add_user(req, userQueue);
    });

    // Get user route (GET /get-user)
    CROW_ROUTE(app, "/get-user").methods("GET"_method)([](){
        return handle_get_user(userQueue);
    });

    // Health check route (GET /healthz)
    CROW_ROUTE(app, "/healthz").methods("GET"_method)([](){
        return handle_health_check();
    });

    // Metrics route (GET /metrics)
    CROW_ROUTE(app, "/metrics").methods("GET"_method)([](){
        return handle_metrics(userQueue);
    });

    app.port(8080).multithreaded().run();
}
