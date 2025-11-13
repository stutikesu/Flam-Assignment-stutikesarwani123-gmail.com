cmake_minimum_required(VERSION 3.22.1)
project(edge_native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(OPENCV_ANDROID_SDK $ENV{OPENCV_ANDROID_SDK})
if(OPENCV_ANDROID_SDK)
    set(OpenCV_DIR "${OPENCV_ANDROID_SDK}/sdk/native/jni")
endif()

find_package(OpenCV REQUIRED core imgproc)

add_library(
    edge_native
    SHARED
    native-lib.cpp
    EdgeProcessor.cpp
)

target_include_directories(edge_native PRIVATE ${OpenCV_INCLUDE_DIRS})

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    edge_native
    PRIVATE
    ${OpenCV_LIBS}
    ${log-lib}
    ${android-lib}
)
