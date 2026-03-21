cmake_minimum_required(VERSION 3.22.1)

project("pblock")

# Add your C++ source files here
add_library(
    pblock
    SHARED
    mainPBLOCK.cpp
    # Add more .cpp files here if needed
)

# Find and link Android libraries
find_library(log-lib log)
find_library(android-lib android)

# Link libraries
target_link_libraries(
    pblock
    ${log-lib}
    ${android-lib}
)

# Set C++ standard
set_target_properties(pblock PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
)
