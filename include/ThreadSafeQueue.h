#ifndef THREADSAFEQUEUE_H
#define THREADSAFEQUEUE_H

#include <queue>
#include <mutex>
#include <condition_variable>

template<typename T>
class ThreadSafeQueue {
private:
    std::queue<T> queue;
    std::mutex mtx;
    std::condition_variable cv;
    size_t max_size = 10000;

public:
    bool enqueue(T element) {
        std::unique_lock<std::mutex> lock(mtx);
        if (queue.size() >= max_size) {
            return false;
        }
        queue.push(element);
        cv.notify_one();
        return true;
    }

    bool dequeue(T &element) {
        std::unique_lock<std::mutex> lock(mtx);
        if (queue.empty()) {
            return false;
        }
        element = queue.front();
        queue.pop();
        return true;
    }

    size_t size() {
        std::unique_lock<std::mutex> lock(mtx);
        return queue.size();
    }
};

#endif
