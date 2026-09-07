# Ride Matching Engine

A Java-based ride-matching engine that simulates the core backend logic behind real-time ride-hailing systems.

The project focuses on **algorithmic efficiency, geospatial matching, concurrency, and scalable backend design** rather than building a traditional CRUD application.

## Current Status

**In development**

The current implementation supports:

* Driver representation with real-time locations
* Ride request representation
* Driver availability
* Nearest-driver matching (linear scan baseline)
* Haversine-based geographic distance calculation (accurate, in kilometers)
* Geographic coordinate validation
* Optional max-distance matching (ignore drivers beyond a given radius)
* Spatial grid index for fast nearest-driver lookup
* Benchmark suite comparing linear scan vs. spatial grid

Future versions will introduce:

* Concurrent driver location updates
* Concurrent ride requests
* Thread-safe driver matching
* Load testing at larger scale
* Additional performance metrics

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

There are two interchangeable ways to go from "Available Drivers" to "Nearest Driver": a linear scan (`MatchingEngine`) and an indexed lookup (`SpatialGrid`). Both are kept side by side so their results and performance can be directly compared.

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
[Linear scan]  Matched driver: 1
[Spatial grid] Matched driver: 1
```

Both engines agree.

## Project Structure

```text
src/main/java/
├── Main.java
├── Location.java
├── Driver.java
├── RideRequest.java
├── MatchingEngine.java
├── SpatialGrid.java
├── GridVsLinearScanTest.java
└── Benchmark.java
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

The baseline: a linear scan over available drivers, using Haversine distance. Time complexity O(n). Kept as-is so it can act as ground truth for verifying the spatial grid's results.

### `SpatialGrid`

 Buckets drivers into geographic cells so a ride request only has to examine drivers near the pickup point.

Uses an **expanding ring search**: it checks the pickup's own cell, then the ring of cells around it, then the next ring out, and so on — stopping as soon as it can *prove* no closer driver could exist further out. This makes it provably equivalent to the linear scan rather than an approximation that's merely "usually right."

### `GridVsLinearScanTest`

A correctness test (no external test framework required) that runs both engines against the same randomized scenarios — dense drivers, sparse drivers, drivers right at a cell boundary, max-distance cutoffs, no drivers available — and asserts they always agree.

### `Benchmark`

Measures matching latency for both engines as driver count grows from 100 to 500,000, and prints a comparison table. See [Benchmark Results](#benchmark-results) below.

## Benchmark Results

Measured on a ~55km x 55km service area (Amsterdam metro scale), 500 requests per driver-count, JIT-warmed before timing:

| Drivers | Linear scan (avg µs/match) | Spatial grid (avg µs/match) | Speedup |
|---------|----------------------------|------------------------------|---------|
| 100     | 7.80                       | 4.36                         | 1.8x    |
| 1,000   | 58.70                      | 4.46                         | 13.2x   |
| 10,000  | 579.93                     | 13.81                        | 42.0x   |
| 100,000 | 7725.32                    | 120.76                       | 64.0x   |
| 500,000 | 29371.81                   | 1086.20                      | 27.0x   |

The linear scan's latency grows proportionally with driver count, exactly as its O(n) complexity predicts. The spatial grid's latency grows far more slowly, since it only examines drivers in nearby cells — the gap between the two widens as the system scales, which is the entire motivation for building the index in the first place.

## Complexity

| Engine | Time | Space |
|---|---|---|
| `MatchingEngine` (linear scan) | O(n) | O(1) |
| `SpatialGrid` (indexed) | O(k), where k = drivers in nearby cells (k ≪ n in practice) | O(n) to store the index |

where `n` is the total number of drivers.

The spatial grid trades memory (it must store an index of every driver's cell) for time (it no longer needs to touch every driver per request) — a classic space/time trade-off, not a free win.

## Technology

* Java 21
* Object-Oriented Programming
* Java Collections
* Algorithms & Data Structures
* Git

No external frameworks are currently used.

## Goal

The goal of this project is to explore the engineering challenges involved in building a **high-throughput, low-latency ride-matching system**.

The project is inspired by the types of problems encountered in large-scale transportation and marketplace platforms, where thousands of drivers can update their locations while many ride requests are processed concurrently.

---

## Running This Project

No build tool (Maven/Gradle) is required — everything runs with the JDK directly.

**Requirements:** Java 21 (check with `java -version` / `javac -version`).

### 1. Compile everything

From the project root (the folder containing all the `.java` files):

```bash
javac *.java -d out
```

This compiles every class into an `out/` directory.

### 2. Run the demo (`Main`)

```bash
java -cp out Main
```

**Expected output:**

```text
[Linear scan]  Matched driver: 1
[Spatial grid] Matched driver: 1
```

Both engines are given the same three drivers and the same ride request, and both return driver 1 — showing they agree on a simple case.

### 3. Run the correctness test (`GridVsLinearScanTest`)

```bash
java -cp out GridVsLinearScanTest
```

**Expected output:** a `PASS` line for each of 8 scenarios (dense drivers, sparse drivers, a cell-boundary edge case, max-distance cutoffs, and a no-drivers-available case), followed by a summary:

```text
PASS  Dense placement, 500 drivers x 300 requests  (0 mismatches out of 300 requests)
PASS  Sparse placement, 30 drivers x 300 requests  (0 mismatches out of 300 requests)
...
8 passed, 0 failed
```

If any scenario fails, it prints a `FAIL` line with the mismatch details, and the process exits with a non-zero status code (useful if you wire this into CI later).

### 4. Run the benchmark (`Benchmark`)

```bash
java -cp out Benchmark
```

This generates random drivers and ride requests at five different scales (100 up to 500,000 drivers), times both engines, and prints a results table like the one in [Benchmark Results](#benchmark-results) above.

Note: the 500,000-driver run allocates a fair amount of memory for driver objects and grid cells. If it's slow or you hit a `GC overhead` warning on a memory-constrained machine, run it with a larger heap:

```bash
java -Xmx2g -cp out Benchmark
```

### Quick reference

```text
javac *.java -d out                 # compile
java -cp out Main                   # run the demo
java -cp out GridVsLinearScanTest   # run correctness tests
java -cp out Benchmark              # run the performance benchmark
```
