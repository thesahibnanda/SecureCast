#ifndef HANDLERS_H
#define HANDLERS_H

#include <crow.h>
#include "ThreadSafeQueue.h"

crow::response handle_add_user(const crow::request& req, ThreadSafeQueue<std::string>& userQueue);

crow::response handle_get_user(ThreadSafeQueue<std::string>& userQueue);

crow::response handle_health_check();

crow::response handle_metrics(ThreadSafeQueue<std::string>& userQueue);

#endif
