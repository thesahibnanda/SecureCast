use std::collections::VecDeque;
use std::sync::Mutex;

pub struct ThreadSafeQueue<T> {
    queue: Mutex<VecDeque<T>>,
    max_size: usize,
}

impl<T> ThreadSafeQueue<T> {
    pub fn new(max_size: usize) -> Self {
        ThreadSafeQueue {
            queue: Mutex::new(VecDeque::new()),
            max_size,
        }
    }

    pub fn enqueue(&self, item: T) -> bool {
        let mut queue = self.queue.lock().unwrap();
        if queue.len() >= self.max_size {
            false
        } else {
            queue.push_back(item);
            true
        }
    }

    pub fn dequeue(&self) -> Option<T> {
        let mut queue = self.queue.lock().unwrap();
        queue.pop_front()
    }

    pub fn size(&self) -> usize {
        let queue = self.queue.lock().unwrap();
        queue.len()
    }
}
