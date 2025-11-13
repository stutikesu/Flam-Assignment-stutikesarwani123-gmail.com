#pragma once

#include <cstdint>
#include <mutex>
#include <vector>

class EdgeProcessor {
  public:
    EdgeProcessor();

    void configure(int width, int height);
    std::vector<uint8_t> process(const std::vector<uint8_t>& nv21, int width, int height);
    void setMode(bool edgeMode);

  private:
    std::mutex mutex_;
    bool edgeMode_ = true;
    int configuredWidth_ = 0;
    int configuredHeight_ = 0;
};
