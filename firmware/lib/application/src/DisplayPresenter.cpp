#include "DisplayPresenter.h"

#include <cstring>

namespace cisc {

void DisplayPresenter::tick() {
  const HouseSnapshot snapshot = house_.snapshot();
  if (drawn_ && !hasElapsed(snapshot.uptimeMs, lastCheckAt_, refreshMs_)) return;
  lastCheckAt_ = snapshot.uptimeMs;

  const DisplayText text = formatDisplay(snapshot);
  if (drawn_ && std::strcmp(text.top, shown_.top) == 0 && std::strcmp(text.bottom, shown_.bottom) == 0) return;

  display_.show(text.top, text.bottom);
  shown_ = text;
  drawn_ = true;
}

}  // namespace cisc
