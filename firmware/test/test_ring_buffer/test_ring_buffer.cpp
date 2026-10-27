#include <unity.h>

#include "RingBuffer.h"
#include "Sample.h"

using cisc::RingBuffer;
using cisc::Sample;

void test_starts_empty() {
  const RingBuffer<int, 3> buffer;
  TEST_ASSERT_EQUAL_UINT(0, buffer.size());
  TEST_ASSERT_EQUAL_UINT(3, buffer.capacity());
}

void test_keeps_insertion_order_while_not_full() {
  RingBuffer<int, 3> buffer;
  buffer.push(10);
  buffer.push(20);
  TEST_ASSERT_EQUAL_UINT(2, buffer.size());
  TEST_ASSERT_EQUAL_INT(10, buffer.at(0));
  TEST_ASSERT_EQUAL_INT(20, buffer.at(1));
}

void test_overwrites_the_oldest_when_full() {
  RingBuffer<int, 3> buffer;
  for (int value = 1; value <= 5; ++value) buffer.push(value);
  TEST_ASSERT_EQUAL_UINT(3, buffer.size());
  TEST_ASSERT_EQUAL_INT(3, buffer.at(0));
  TEST_ASSERT_EQUAL_INT(4, buffer.at(1));
  TEST_ASSERT_EQUAL_INT(5, buffer.at(2));
}

void test_stays_ordered_after_wrapping_many_times() {
  RingBuffer<int, 4> buffer;
  for (int value = 1; value <= 100; ++value) buffer.push(value);
  TEST_ASSERT_EQUAL_INT(97, buffer.at(0));
  TEST_ASSERT_EQUAL_INT(100, buffer.at(3));
}

void test_clear_empties_the_buffer() {
  RingBuffer<int, 3> buffer;
  buffer.push(1);
  buffer.push(2);
  buffer.clear();
  TEST_ASSERT_EQUAL_UINT(0, buffer.size());
  buffer.push(7);
  TEST_ASSERT_EQUAL_INT(7, buffer.at(0));
}

void test_stores_samples() {
  RingBuffer<Sample, 2> history;
  history.push(Sample{1000, 120, 21.5f, 74.0f, true});
  history.push(Sample{2000, 130, 0.0f, 0.0f, false});
  TEST_ASSERT_EQUAL_UINT32(1000, history.at(0).timestampMs);
  TEST_ASSERT_EQUAL_INT(120, history.at(0).gasReading);
  TEST_ASSERT_EQUAL_FLOAT(74.0f, history.at(0).humidityPercent);
  TEST_ASSERT_FALSE(history.at(1).climateValid);
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_starts_empty);
  RUN_TEST(test_keeps_insertion_order_while_not_full);
  RUN_TEST(test_overwrites_the_oldest_when_full);
  RUN_TEST(test_stays_ordered_after_wrapping_many_times);
  RUN_TEST(test_clear_empties_the_buffer);
  RUN_TEST(test_stores_samples);
  return UNITY_END();
}
