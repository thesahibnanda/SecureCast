#include <signal.h>
#include <spdlog/spdlog.h>

void signal_handler(int signum) {
    spdlog::info("Interrupt signal ({}) received. Shutting down...", signum);
    exit(0);
}
