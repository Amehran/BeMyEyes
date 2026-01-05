# Backend Architecture Design: "Eye-Mind" Service

## 1. Project Structure
The backend will be a modular **Python** service built on **FastAPI**. It is designed for concurrency (multiple users streaming data) and extensibility (adding new Agents easily).

```text
/backend-service
├── app/
│   ├── main.py            # Entry point (FastAPI)
│   ├── core/              # Config, Security (API Keys)
│   ├── router/            # API Endpoints (/analyze, /stream)
│   ├── modules/           # The "Brain" Components
│   │   ├── orchestrator.py    # The Decision Maker
│   │   ├── agents/            # Specialized Skills
│   │   │   ├── base_agent.py  # Interface
│   │   │   ├── navigation.py  # Outdoor/Pathfinding
│   │   │   ├── reading.py     # OCR/Document
│   │   │   └── describer.py   # General VQA (Gemini Pro)
│   │   └── models/            # Pydantic Schemas (Input/Output)
│   └── utils/
│       └── image_processing.py # Resize, Normalize
├── tests/
└── requirements.txt
```

---

## 2. The Core Components

### A. The Orchestrator (`modules/orchestrator.py`)
*   **Role:** The Logic Gate. It prevents wasted money and time by routing requests effectively.
*   **Input:** `UserRequest(image, type, telemetry)`
*   **Logic:**
    1.  **Check explicit type:** If user clicked "Read Mode" -> Route directly to `ReadingAgent`.
    2.  **Check telemetry:** If moving > 5km/h -> Route directly to `NavigationAgent`.
    3.  **Fallback:** If generic -> Send to light classifier -> Route.

### B. The Agents (`modules/agents/`)

#### 1. Navigation Agent (`Gemini Flash` / `GPT-4o-mini`)
*   **Prompt Personality:** "You are a precise mobility guide. Do not be poetic. Identify only obstacles, path boundaries, and safety signals."
*   **Output Format:** JSON `{"hazard": "Pole", "direction": "12 o'clock", "distance": "2m"}`.

#### 2. Reading Agent (`Google Cloud Vision` OR `Gemini Pro 1.5`)
*   **Strategy:** Pure OCR is safer for important documents (Bank letters) to avoid hallucination. For menus/signs, LLM is better.
*   **Logic:**
    *   Step 1: Detect text density.
    *   Step 2: If dense -> OCR. If sparse -> LLM Summary.

#### 3. Describer Agent (`Gemini Pro Vision 1.5`)
*   **Prompt Personality:** "Describe the scene in rich detail, focusing on atmosphere, colors, and people."
*   **Usage:** Only for "General" queries.

---

## 3. The API Contract (Schema)

**Endpoint:** `POST /v1/analyze`

**Request Body:**
```json
{
  "image_base64": "...",
  "user_intent": "NAVIGATION", // enum: [NAVIGATION, READING, GENERAL, AUTO]
  "telemetry": {
     "speed_mps": 1.2,
     "location_type": "OUTDOOR" 
  }
}
```

**Response Body:**
```json
{
  "agent_id": "navigation_agent_v1",
  "speech_response": "Pole directly ahead. Veering left.",
  "haptic_pattern": "DANGER_PULSE",  // Optional
  "suggested_actions": ["Switch to Reading Mode"] 
}
```

---

## 4. Key Libraries
*   **Framework:** `FastAPI` + `Uvicorn` (High throughput).
*   **AI SDK:** `google-generativeai` (Gemini), `langchain` (Agent flow).
*   **Validation:** `Pydantic` (Strict schema validation).
*   **OCR:** `pytesseract` (Backup) or `easyocr`.

## 5. Deployment Strategy
*   **Container:** Dockerized application.
*   **Hosting:** Google Cloud Run (Serverless) -> Costs $0 when idle. Ideal for sporadic usage.
