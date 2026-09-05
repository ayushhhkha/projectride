# Ride Matching Engine

A Java-based ride-matching engine that simulates the core backend logic behind real-time ride-hailing systems.

The project focuses on **algorithmic efficiency, geospatial matching, concurrency, and scalable backend design** rather than building a traditional CRUD application.

## Current Status

**In development**

The current implementation supports:

* Driver representation with real-time locations
* Ride request representation
* Driver availability
* Nearest-driver matching
* Haversine-based geographic distance calculation (accurate, in kilometers)
* Geographic coordinate validation
* Optional max-distance matching (ignore drivers beyond a given radius)

Future versions will introduce:

* Geospatial indexing
* Concurrent driver location updates
* Concurrent ride requests
* Thread-safe driver matching
* Optimized matching algorithms
* Load testing and benchmarking
* Performance metrics

## Architecture

The system currently follows a simple flow:

```text
                    Ride Request
                         │
                         ▼
                 Matching Engine
                         │
                         ▼
                  Available Drivers
                         │
                         ▼
                 Distance Calculation
                         │
                         ▼
                  Nearest Driver
```

## Example

The engine can represent multiple drivers:

```text
Driver 1 → (52.0700, 4.3000)
Driver 2 → (52.0800, 4.3100)
Driver 3 → (52.1000, 4.3300)
```

Given a ride request with a pickup location, the matching engine evaluates available drivers and selects the closest one.

Example output:

```text
Matched driver: 1
```

## Project Structure

```text
src/main/java/
├── Main.java
├── Location.java
├── Driver.java
├── RideRequest.java
└── MatchingEngine.java
```

### `Location`

Represents a driver's or rider's geographic position using latitude and longitude.

Coordinates are validated on construction: latitude must be between -90 and 90 degrees, and longitude between -180 and 180 degrees. Invalid values throw an `IllegalArgumentException`.

### `Driver`

Represents a driver with:

* Unique ID
* Current location
* Availability status

### `RideRequest`

Represents a ride request containing:

* Request ID
* Pickup location
* Destination

### `MatchingEngine`

Responsible for finding the nearest available driver.

The current implementation performs a linear scan over available drivers, using the Haversine formula to compute real-world distance in kilometers between the pickup point and each driver.

An overloaded `findNearestDriver(request, drivers, maxDistanceKm)` method is also available, which ignores drivers farther than `maxDistanceKm` from the pickup — useful for avoiding matches that are technically nearest but unrealistically far away.

## Distance Calculation

Distance is calculated using the **Haversine formula**, which computes the great-circle distance between two latitude/longitude points in kilometers, accounting for the Earth's curvature.

An earlier version used a Euclidean-style calculation (`sqrt(latDiff² + lonDiff²)`), which treated latitude and longitude as flat Cartesian coordinates. That approach produced a unitless number with no real-world meaning and became increasingly inaccurate over longer distances or at higher latitudes, since a degree of longitude covers less physical ground farther from the equator.

Example difference for the same two points (~130m apart in reality):

```text
Old (Euclidean, unitless): 0.001414
New (Haversine, km):       0.1305 km
```

## Complexity

The current matching algorithm has:

```text
Time:  O(n)
Space: O(1)
```

where `n` is the number of available drivers.

For every ride request, the engine currently checks each available driver.

Note: this is still O(n), but each comparison now costs more than the earlier Euclidean version, since Haversine uses trigonometric functions instead of a simple subtraction. This is part of the motivation for Phase 3's geospatial index — it reduces `n` itself rather than making each comparison faster.

This is intentional for the initial implementation. The next major optimization is to introduce a **geospatial index** so that the engine does not need to scan every driver.

## Technology

* Java 21
* Object-Oriented Programming
* Java Collections
* Algorithms & Data Structures
* Git

No external frameworks are currently used.

## Roadmap

### Phase 1 — Basic Matching

* [x] Driver model
* [x] Location model
* [x] Ride request model
* [x] Nearest-driver matching
* [x] Basic distance calculation

### Phase 2 — Geographic Accuracy

* [x] Haversine distance
* [x] Geographic validation
* [x] Distance-based matching

### Phase 3 — Geospatial Indexing

* [ ] Spatial grid
* [ ] Nearby-driver lookup
* [ ] Reduce candidate drivers
* [ ] Benchmark against linear scanning

### Phase 4 — Concurrency

* [ ] Concurrent driver location updates
* [ ] Concurrent ride requests
* [ ] Thread-safe driver state
* [ ] Race-condition testing
* [ ] ExecutorService-based simulation

### Phase 5 — Performance & Reliability

* [ ] Load testing
* [ ] Benchmark matching latency
* [ ] Simulate thousands of drivers
* [ ] Measure throughput
* [ ] Optimize memory usage

## Goal

The goal of this project is to explore the engineering challenges involved in building a **high-throughput, low-latency ride-matching system**.

The project is inspired by the types of problems encountered in large-scale transportation and marketplace platforms, where thousands of drivers can update their locations while many ride requests are processed concurrently.