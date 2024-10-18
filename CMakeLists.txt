cmake_minimum_required(VERSION 3.10)
project(InvisibleQueueProject)

# Set C++ standard
set(CMAKE_CXX_STANDARD 20)

# Manually specify Boost paths
set(BOOST_ROOT "/home/linuxbrew/.linuxbrew/opt/boost")
set(BOOST_INCLUDEDIR "${BOOST_ROOT}/include")
set(BOOST_LIBRARYDIR "${BOOST_ROOT}/lib")

# Find Boost (system component is required for Boost.Asio)
find_package(Boost REQUIRED COMPONENTS system)
if (Boost_FOUND)
    message(STATUS "Boost found: ${Boost_VERSION}")
    include_directories(${Boost_INCLUDE_DIRS})
else()
    message(FATAL_ERROR "Boost not found.")
endif()

# Specify the OpenSSL root directory
set(OPENSSL_ROOT_DIR "/home/linuxbrew/.linuxbrew/opt/openssl@3")
set(OPENSSL_INCLUDE_DIR "${OPENSSL_ROOT_DIR}/include")
set(OPENSSL_LIBRARIES "${OPENSSL_ROOT_DIR}/lib")

# Find OpenSSL for SSL support
find_package(OpenSSL REQUIRED)
if (OpenSSL_FOUND)
    message(STATUS "OpenSSL found: ${OpenSSL_VERSION}")
    include_directories(${OPENSSL_INCLUDE_DIR})
    link_directories(${OPENSSL_LIBRARIES})
else()
    message(FATAL_ERROR "OpenSSL not found.")
endif()

# Include ASIO headers (header-only library)
set(ASIO_INCLUDE_DIR "/home/linuxbrew/.linuxbrew/opt/asio/include")
if (EXISTS ${ASIO_INCLUDE_DIR})
    message(STATUS "ASIO found at ${ASIO_INCLUDE_DIR}.")
    include_directories(${ASIO_INCLUDE_DIR})
else()
    message(FATAL_ERROR "ASIO not found.")
endif()

# Include Crow headers (header-only library)
set(CROW_INCLUDE_DIR "/home/linuxbrew/.linuxbrew/opt/crow/include")
if (EXISTS ${CROW_INCLUDE_DIR})
    message(STATUS "Crow found at ${CROW_INCLUDE_DIR}.")
    include_directories(${CROW_INCLUDE_DIR})
else()
    message(FATAL_ERROR "Crow not found.")
endif()

# Include SPDLog headers
set(SPDLOG_INCLUDE_DIR "/home/linuxbrew/.linuxbrew/opt/spdlog/include")
if (EXISTS ${SPDLOG_INCLUDE_DIR})
    message(STATUS "SPDLog found at ${SPDLOG_INCLUDE_DIR}.")
    include_directories(${SPDLOG_INCLUDE_DIR})
else()
    message(FATAL_ERROR "SPDLog not found.")
endif()

# Include fmt library (dependency of spdlog)
set(FMT_INCLUDE_DIR "/home/linuxbrew/.linuxbrew/opt/fmt/include")
set(FMT_LIBRARY "/home/linuxbrew/.linuxbrew/opt/fmt/lib")

if (EXISTS ${FMT_INCLUDE_DIR} AND EXISTS ${FMT_LIBRARY})
    message(STATUS "fmt found at ${FMT_INCLUDE_DIR}.")
    include_directories(${FMT_INCLUDE_DIR})
    link_directories(${FMT_LIBRARY})
else()
    message(FATAL_ERROR "fmt not found.")
endif()

# Include project headers
include_directories(include)

# Add executable and include source files from the src directory
add_executable(InvisibleQueueProject
        src/main.cpp
        src/handlers.cpp
        src/signal_handler.cpp
)

# Link necessary libraries
target_link_libraries(InvisibleQueueProject
        Boost::system       # Boost system component
        OpenSSL::SSL        # OpenSSL SSL library
        OpenSSL::Crypto     # OpenSSL Crypto library
        pthread             # pthread for multithreading
        fmt                 # Link fmt library
)
