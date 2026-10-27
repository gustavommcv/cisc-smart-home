#pragma once

#include "DisplayText.h"
#include "HouseController.h"
#include "Ports.h"
#include "Time.h"

namespace cisc {

// Keeps a display up to date with the house. It looks at the house at its own
// pace and touches the display only when the text actually changed, so a slow
// bus never holds up the control loop and the screen never flickers.
class DisplayPresenter {
 public:
  DisplayPresenter(const HouseController& house, Display& display, Millis refreshMs)
      : house_(house), display_(display), refreshMs_(refreshMs) {}

  // Call from the main loop, after HouseController::tick().
  void tick();

 private:
  const HouseController& house_;
  Display& display_;
  Millis refreshMs_;

  bool drawn_ = false;
  Millis lastCheckAt_ = 0;
  DisplayText shown_{};
};

}  // namespace cisc
