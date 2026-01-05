# Phase 7: Live Mapping (SLAM Lite)
**Goal:** Enable the AI to understand relative position/orientation of objects by fusing Vision with Compass/Gyroscope data.

## 7.1. Backend Updates (First)
### A. Schema Update
*   Update `Telemetry` model in `request_response.py`.
    *   Add `heading: float` (0-360 degrees).
    *   Add `pitch: float` (-90 to +90 degrees).

### B. Map Service (New Component)
*   Create `app/services/mapping.py`.
*   **Logic:**
    *   Maintain a temporary session map (in-memory or short-term DB).
    *   Function `update_map(user_id, heading, objects_found)`.
    *   Function `get_relative_positions(user_id, current_heading)`.

### C. TDD Strategy (Backend)
1.  Create `tests/test_map_service.py` **FIRST**.
2.  Test `calculate_relative_bearing(object_angle, user_heading)`.
3.  Test `store_object_location`.

---

## 7.2. Android Client Updates
### A. Sensor Manager
*   Implement `OrientationManager` class using Android Sensor API.
*   Smooth the compass data (Low-pass filter) to avoid jitter.

### B. API Integration
*   Inject `heading/pitch` into `AnalysisRequest` via `BackendRepository`.
