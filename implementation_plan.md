# Implementation Plan: Smart Vision Refactor (TDD Approach)

## Phase 1: Infrastructure & Baseline Restoration
**Goal:** Clean up the "reckless" changes and establish a stable base with necessary dependencies.
- [ ] Revert logic changes in `CameraViewModel.kt` and `DetectorModule.kt`.
- [ ] Remove the untracked `MediaPipeObjectDetector.kt` to start fresh.
- [ ] Keep and verify `libs.versions.toml` and `build.gradle.kts` changes (MediaPipe dependencies).
- [ ] **Action:** Commit `build: add MediaPipe dependencies`.

## Phase 2: Fix "Chatter" Issue (TDD)
**Goal:** Prevent repetitive audio announcements (e.g., "Chair... Table... Chair...") using a test-driven approach.
- [ ] **Test (Red):** Create `CameraViewModelTest.kt`.
    - Write a test case: `detect_shouldNotSpeak_SameObjectWithinCooldown`.
    - Write a test case (The Fix): `detect_shouldNotSpeak_PreviouslySeenObject_IfRecoveredQuickly`.
- [ ] **Implement (Green):** Modify `CameraViewModel` to track timestamps *per label* instead of a single global timestamp.
- [ ] **Refactor:** Clean up the debounce logic.
- [ ] **Action:** Commit `feat: implement per-object audio debounce`.

## Phase 3: Migrate to MediaPipe (TDD)
**Goal:** Replace the raw TFLite implementation with MediaPipe Tasks for better stability and confidence.
- [ ] **Test (Red):** Create `MediaPipeObjectDetectorTest.kt` (Unit).
    - Test mapping logic: Verify that a raw MediaPipe result is correctly converted to our `Detection` domain model.
    - Test filtering: Verify that non-whitelisted labels (like "tie" or "book") are ignored.
- [ ] **Implement (Green):** Create `MediaPipeObjectDetector` class that satisfies the tests.
- [ ] **Integration:** Update `DetectorModule.kt` to inject the new detector.
- [ ] **Action:** Commit `refactor: replace TFLite with MediaPipe detector`.

## Phase 4: System Verification
- [ ] Build the app (`./gradlew assembleDebug`).
- [ ] Verify no regressions in existing tests.
