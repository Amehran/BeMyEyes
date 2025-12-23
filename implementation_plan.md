# Implementation Plan: Smart Vision Refactor (TDD Approach)

## Phase 1: Infrastructure & Baseline Restoration
**Goal:** Clean up the "reckless" changes and establish a stable base with necessary dependencies.
- [x] Revert logic changes in `CameraViewModel.kt` and `DetectorModule.kt`.
- [x] Remove the untracked `MediaPipeObjectDetector.kt` to start fresh.
- [x] Keep and verify `libs.versions.toml` and `build.gradle.kts` changes (MediaPipe dependencies).
- [x] **Action:** Commit `build: add MediaPipe dependencies`.

## Phase 2: Fix "Chatter" Issue (TDD)
**Goal:** Prevent repetitive audio announcements (e.g., "Chair... Table... Chair...") using a test-driven approach.
- [x] **Test (Red):** Create `CameraViewModelTest.kt`.
    - Write a test case: `detect_shouldNotSpeak_SameObjectWithinCooldown`.
    - Write a test case (The Fix): `detect_shouldNotSpeak_PreviouslySeenObject_IfRecoveredQuickly`.
- [x] **Implement (Green):** Modify `CameraViewModel` to track timestamps *per label* instead of a single global timestamp.
- [x] **Refactor:** Clean up the debounce logic.
- [x] **Action:** Commit `feat: implement per-object audio debounce`.

## Phase 3: Migrate to MediaPipe (TDD)
**Goal:** Replace the raw TFLite implementation with MediaPipe Tasks for better stability and confidence.
- [x] **Test (Red):** Create `MediaPipeObjectDetectorTest.kt` (Unit).
    - Test mapping logic: Verify that a raw MediaPipe result is correctly converted to our `Detection` domain model.
    - Test filtering: Verify that non-whitelisted labels (like "tie" or "book") are ignored.
- [x] **Implement (Green):** Create `MediaPipeObjectDetector` class that satisfies the tests.
- [x] **Integration:** Update `DetectorModule.kt` to inject the new detector.
- [x] **Action:** Commit `refactor: replace TFLite with MediaPipe detector`.

## Phase 4: System Verification
- [x] Build the app (`./gradlew assembleDebug`).
- [x] Verify no regressions in existing tests.

## Phase 4: Temporal Smoothing (TDD)
**Goal:** Implement a "Buffer" to ignore 1-frame flickers and only report stable objects.
- [x] **Test (Red):** Create `DetectionTrackerTest.kt`.
    - Test: `process_shouldWait3Frames_BeforeReportingNewObject()`.
    - Test: `process_shouldKeepObjectAlive_IfMissingFor1Frame()`.
    - Test: `process_shouldDropObject_IfMissingFor5Frames()`.
- [x] **Implement (Green):** Create `DetectionTracker` class (Pure Kotlin logic, no Android dependencies).
- [x] **Integration:** Hook `DetectionTracker` into `CameraViewModel`.
- [x] **Action:** Commit `feat: add DetectionTracker for temporal smoothing`.

## Phase 5: Contextual Intelligence (TDD)
**Goal:** Group objects into a meaningful sentence instead of shouting single words.
- [ ] **Test (Red):** Create `SceneDescriberTest.kt`.
    - Test: `describe_shouldGroupObjects_LikeChairAndTable()`.
    - Test: `describe_shouldPrioritizeUrgentObjects_AtStartOfSentinel()`.
- [ ] **Implement (Green):** Create `SceneDescriber` class.
- [ ] **Integration:** Replace the single-object speech logic in `CameraViewModel` with `SceneDescriber.describe(List<Detection>)`.
- [ ] **Action:** Commit `feat: implement scene description logic`.
