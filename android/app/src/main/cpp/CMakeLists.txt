cmake_minimum_required(VERSION 3.22.1)

project(l2c_fcr_hook)

set(CMAKE_CXX_STANDARD 17)

add_library(l2c_fcr_hook SHARED
        l2c_fcr_hook.cpp)

target_link_libraries(l2c_fcr_hook
        android
        log)
