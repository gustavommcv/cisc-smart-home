#pragma once

#include <cstddef>

namespace cisc {

// Fixed-size history: once full, each new item replaces the oldest one.
// No heap allocation, so it is safe on a microcontroller.
template <typename T, std::size_t Capacity>
class RingBuffer {
  static_assert(Capacity > 0, "a ring buffer needs room for at least one item");

 public:
  void push(const T& item) {
    items_[next_] = item;
    next_ = (next_ + 1) % Capacity;
    if (size_ < Capacity) ++size_;
  }

  void clear() {
    next_ = 0;
    size_ = 0;
  }

  std::size_t size() const { return size_; }
  static constexpr std::size_t capacity() { return Capacity; }

  // Index 0 is the oldest item. `index` must be below size().
  const T& at(std::size_t index) const {
    return items_[(next_ + Capacity - size_ + index) % Capacity];
  }

 private:
  T items_[Capacity] = {};
  std::size_t next_ = 0;
  std::size_t size_ = 0;
};

}  // namespace cisc
