# Phase 8: Social Intelligence
**Goal:** Recognize known individuals and interpret emotional states.

## 8.1. Backend: The Social Agent
*   **New Agent:** `SocialAgent` in `app.agents.perception.social`.
*   **Capabilities:**
    1.  **Emotion Detection:** Analyze facial expressions (Gemini).
    2.  **Identity Matching:** (MVP) Use Gemini context or a lightweight embedding Matcher.
    
### Technical Strategy for Identity (MVP)
Since installing `dlib`/`face_recognition` on Cloud Run can be complex (compilation issues), we will initially leverage **Gemini's Context Window**.
1.  **User Enrollment:** User says "This is Alice." -> We save a high-quality textual description of Alice into `memories` table (e.g., "Alice has short brown hair and wears red glasses").
2.  **Recognition:** When `SocialAgent` runs, it queries `memories` for "People" descriptions.
3.  **Prompt:** "Here is an image. Do you see anyone matching these descriptions: [Alice: short brown hair...]? If so, identify them."

*Note: True Biometric Face Auth is Phase 9. Phase 8 is "Descriptive Recognition".*

## 8.2. Android Integration
*   **Intent:** "Who is this?" -> Routes to `SocialAgent`.
*   **Enrollment:** "Remember this person is Alice." -> Routes to `MemoryService` (via `SocialAgent` or `Orchestrator`).

## 8.3. TDD Strategy
*   `test_social_agent.py`:
    *   Mock LLM response for "Happy Face".
    *   Mock Memory retrieval of "Alice".
    *   Verify final output says "This looks like Alice, and she seems happy."
