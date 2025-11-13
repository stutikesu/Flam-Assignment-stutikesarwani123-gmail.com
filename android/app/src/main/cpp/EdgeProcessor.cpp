#include "EdgeProcessor.h"

#include <android/log.h>
#include <opencv2/imgproc.hpp>
#include <vector>
#include <cstring>

namespace {
constexpr char kTag[] = "EdgeProcessor";
constexpr double kLowerThreshold = 60.0;
constexpr double kUpperThreshold = 140.0;
}

EdgeProcessor::EdgeProcessor() = default;

void EdgeProcessor::configure(int width, int height) {
    std::lock_guard<std::mutex> lock(mutex_);
    configuredWidth_ = width;
    configuredHeight_ = height;
    __android_log_print(ANDROID_LOG_INFO, kTag, "Configured %dx%d", width, height);
}

std::vector<uint8_t> EdgeProcessor::process(
        const std::vector<uint8_t>& nv21,
        int width,
        int height) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (width <= 0 || height <= 0 || nv21.empty()) {
        return {};
    }

    cv::Mat yuv(height + height / 2, width, CV_8UC1, const_cast<uint8_t*>(nv21.data()));
    cv::Mat rgba;
    cv::cvtColor(yuv, rgba, cv::COLOR_YUV2RGBA_NV21);

    if (edgeMode_) {
        cv::Mat gray;
        cv::cvtColor(rgba, gray, cv::COLOR_RGBA2GRAY);
        cv::Mat edges;
        cv::Canny(gray, edges, kLowerThreshold, kUpperThreshold);
        cv::cvtColor(edges, rgba, cv::COLOR_GRAY2RGBA);
    }

    const size_t totalBytes = static_cast<size_t>(rgba.total() * rgba.elemSize());
    std::vector<uint8_t> out(totalBytes);
    if (totalBytes > 0) {
        std::memcpy(out.data(), rgba.data, totalBytes);
    }
    return out;
}

void EdgeProcessor::setMode(bool edgeMode) {
    std::lock_guard<std::mutex> lock(mutex_);
    edgeMode_ = edgeMode;
}
