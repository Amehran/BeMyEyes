# Roadmap V3: The Cognitive Leap

**Status:** Planning Phase
**Focus:** Memory, Persistence, and Advanced Telemetry

---

## Phase 6: Cognitive Persistence (The Hippocampus)
**Goal:** Transition from "Session Memory" (RAM) to "Long-Term Memory" (Database). The agent should remember object locations and user preferences across app restarts.

### 6.1. Infrastructure (Database Layer)
*   [x] **Selection:** **Supabase** (Postgres + `pgvector`).
*   [ ] **Embedding Model:** Google Gemini (`models/text-embedding-004`) for generating vectors.
*   [ ] **Schema Design:**
    *   `memories` table:
        *   `id` (uuid)
        *   `user_id` (string)
        *   `content` (text) - e.g. "Keys are on the table"
        *   `embedding` (vector[768]) - Semantic representation
        *   `created_at` (timestamp)

### 6.2. The Memory Agent (New)
*   [ ] Create `MemoryService`.
    *   `store(user_id, text, type)`
    *   `recall(user_id, query_embedding)`
*   [ ] Integration: Connect `AwarenessAgent` to `MemoryService`.

### 6.3. Client identification
*   [ ] **Android:** Generate stable `installation_id` (UUID).
*   [ ] **Protocol:** Send `X-User-ID` header in all `AnalysisRequest` calls.

---

## Phase 7: Live Mapping (SLAM Lite)
**Goal:** Build a rough 2D mental map of the user's surroundings.

*   [ ] **Telemetry:** Real-time stream of Compass/GPS/Accelerometer.
*   [ ] **Backend:** `MapService` to plot objects on a relative grid (0,0 is user start).

## Phase 8: Social Intelligence
**Goal:** Recognize faces and social cues.

*   [ ] **Face DB:** "That is your brother."
*   [ ] **Emotion Detection:** "He looks happy."
