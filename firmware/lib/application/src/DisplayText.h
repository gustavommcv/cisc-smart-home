#pragma once

#include "HouseSnapshot.h"

namespace cisc {

// What a 16x2 character display shows: two lines of at most 16 characters.
struct DisplayText {
  char top[17];
  char bottom[17];
};

// Decides what to write on the display for a given state of the house. The
// messages are for the people in the house, so they are in Portuguese, without
// accents because the display's character set has none.
DisplayText formatDisplay(const HouseSnapshot& snapshot);

}  // namespace cisc
