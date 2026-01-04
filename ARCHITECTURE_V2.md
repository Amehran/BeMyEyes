# Architecture V2: Mission-Driven Agentic System

## 1. Vision & Philosophy
The goal is to transition from generalist "Describe Image" agents to **Specialized, Mission-Driven Agents**. 
For a blind user, the priority is not just "what is in the image," but **"what should I do?"** and **"am I safe?"**

### Core Principles
1.  **Safety First (Guardian Layer):** Immediate threats override all other tasks.
2.  **Context Specificity:** Walking indoors is fundamentally different from crossing a street.
3.  **Action Oriented:** Output should be navigational directives or specific answers, not just generic descriptions.
4.  **Mode Adaptability:** The system switches between "Planning Mode" (Approximation) and "Pilot Mode" (Real-time obstacle avoidance).

---

## 2. The Guardian Layer (Safety)
**Role:** The "Reflex System." It runs logically parallel to or ahead of other agents.
*   **Mission:** Detect immediate physical threats (Fast cars, drop-offs, construction, red lights).
*   **Behavior:** If a threat is detected, it **interrupts** any other ongoing description to shout a warning.
*   **Output:** Short, imperative commands. "STOP!", "Vehicle approaching left.", "Cliff ahead."

---

## 3. Specialized Agent Hierarchy

### A. Navigation Cluster
*Focus: Movement, Pathfinding, Obstacle Avoidance*

#### 1. Indoor Navigator
*   **Context:** Homes, Offices, Malls.
*   **Focus:** Micro-navigation. Doors, hallways, furniture, tight corners, elevators.
*   **Output Styles:**
    *   *Pilot Mode (Cluttered):* "Chair 2 steps ahead. Side step right."
    *   *Guidance Mode (Clear):* "Hallway is clear. Walk straight approx 10 meters."

#### 2. Outdoor Navigator
*   **Context:** Sidewalks, Parks, Open spaces.
*   **Focus:** Macro-navigation. Sidewalk boundaries, poles, curbs, staying on path.
*   **Output Styles:**
    *   *Pilot Mode:* "Veer left, you are drifting off the sidewalk."
    *   *Guidance Mode:* "Follow this sidewalk for about 50 meters."

#### 3. Crosswalk Assistant (High Risk)
*   **Context:** Street Intersections.
*   **Mission:** Dedicated specifically to the complex task of crossing.
*   **Focus:** Traffic lights (color/state), Traffic flow, Crosswalk lines.

### B. Perception Cluster
*Focus: Identification, Search, Reading*

#### 4. Object Finder (The Seeker)
*   **Mission:** "Find X".
*   **Stateful Behavior:** The agent enters a "Seeking Loop" looking for a specific target (e.g., "Restroom Sign", "Empty Seat").
*   **Output:** Vector directions. "10 o'clock, 3 meters away," or "Scanning... not seen yet."

#### 5. Scene Describer (The Narrator)
*   **Mission:** Ambient awareness.
*   **Configurable Detail:**
    *   *Level 1 (Brief):* "You are in a coffee shop."
    *   *Level 2 (Standard):* "A coffee shop. Counter is on the right, tables to the left."
    *   *Level 3 (Rich):* Full aesthetic description for immersion.

#### 6. Reading Agent (The Lector)
*   **Mission:** Extract meaningful text.
*   **Intelligence:** Distinguish between *Critical Text* (Menu, Sign, Warning) and *Noise* (Graffiti, generic branding).

---

## 4. Orchestration Logic
The **Orchestrator** becomes a smart router using `User Intent` + `Telemetry` + `Visual Context`.

### The Routing Flow
1.  **Safety Check:** (Optional Pre-check) Is there immediate danger? -> **Guardian**.
2.  **Intent Check:** Did the user ask "Where is the exit?" -> **Object Finder**.
3.  **Context Check (Auto-Mode):** 
    *   If `User Intent == AUTO` and `Telemetry.speed > 0.5m/s`:
        *   Classify Environment (Indoor vs Outdoor).
        *   Route to **IndoorNavigator** or **OutdoorNavigator**.
    *   If `User Intent == AUTO` and `Telemetry.speed == 0`:
        *   Route to **Scene Describer**.

---

## 5. Implementation Plan

### Phase 1: Foundation Refactor
*   [x] Create `backend/app/agents/core/` and `backend/app/agents/navigation/`.
*   [x] Define the `Guardian` prompt and logic (can be integrated into base navigation for MVP).
*   [x] Update `AnalysisRequest` schema to support "Target Object" (e.g., `looking_for="keys"`).

### Phase 2: Navigation Split
*   [x] Create `OutdoorNavigatorAgent` (Focus on curbs, paths).
*   [x] Create `IndoorNavigatorAgent` (Focus on doors, furniture).
*   [x] Update `Orchestrator` to switch between them based on a flag or classification.

### Phase 3: The Specialized Object Finder
*   [x] Create `ObjectFinderAgent`.
*   [x] Implement the specific prompt emphasizing "Directional Guidance" (Clock face directions).

### Phase 4: Integration
*   [x] Connect all agents to the `Orchestrator`.
*   [x] Test "Guidance Mode" (Long range) vs "Pilot Mode" (Short range) prompts.

### Phase 5: Awareness Agent (Spatial Memory)
*   [x] Upgrade `DescriberAgent` to `AwarenessAgent`.
*   [x] Implement Spatial Anchoring prompts (Remember where things are).
*   [x] Add "Vision-Only Mode" logic (Implicitly handled by current architecture).
