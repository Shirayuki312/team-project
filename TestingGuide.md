# Plan4Life Testing Checklist (Sections 2–6)

This guide assumes you cloned the repo and can run `plan4life.Main` from an IntelliJ run configuration. Follow each section in order; every step is written for a teammate who has never seen the code before.

## 2. Test the AI scheduling flow (what you are testing)
Confirm that entering a routine description and fixed activities triggers the full AI pipeline and renders a schedule in the Swing calendar grid.

1. Launch the app via IntelliJ (Run → `plan4life.Main`). Wait for the Swing window titled **Plan4Life** to appear.
2. In the bottom panel labeled **Schedule Inputs**, find the **Routine Description** text area.
3. Type one of these examples (or both, one at a time) exactly:
    - "Morning person, 9–5 job, gym 3x/week, dinner with family at 7pm"
    - "Night owl student, classes Tue/Thu 10–12, prefers studying after 8pm"
4. On the right side of **Schedule Inputs**, locate the fixed-activity list fields.
5. Enter at least two fixed activities, then click **Add** after each one:
    - "Mon 09:00-10:00 Team Sync"
    - "Wed 12:00-13:00 Gym" (or "Fri 18:00-19:00 Groceries")
6. Confirm the fixed items appear in the list/table beneath the input fields.
7. Click the **Generate Schedule** button.
8. Expected result if working: The calendar grid repaints with colored blocks for activities. Locked or fixed items show with a lock icon; blocked ranges appear gray. The day/week view buttons should still toggle layouts without losing the new schedule.
9. Error handling: If you leave the routine description empty, a dialog should prompt you (e.g., "Please enter a routine description…"). If generation fails (network/model), expect a dialog like "Unable to generate a schedule right now…" or "No plan could be created…".
10. API key note: Set `HUGGINGFACE_API_KEY` (and optional `HUGGINGFACE_MODEL_ID`) in your run configuration env vars to use the live model. Without it, the LLM service falls back to a deterministic schedule built from your inputs; the UI steps above still work.

## 3. Test RAG retrieval (what you are testing)
Verify that routine keywords pull example routines from the bundled JSON and that those examples influence the prompt sent to the LLM.

1. Open `src/main/resources/ai/examples/routines.json` in IntelliJ to view the bundled examples.
2. Note keywords in the examples (e.g., "morning", "9-5", "student", "night", "exercise").
3. Relaunch the app. In **Routine Description**, type a description that matches one example closely, such as:
    - "Morning runner, 9-5 office, wants evening family time"
    - "College student, late-night study sessions, part-time job"
4. Add one or two fixed activities (e.g., "Tue 14:00-15:00 Lab", "Thu 18:00-19:00 Sports practice") and click **Add** for each.
5. Click **Generate Schedule**.
6. Expected result: The schedule should resemble patterns from the matching example (e.g., morning workouts or late-night study blocks). Colors and blocks should change compared to unrelated descriptions.
7. To verify retrieval explicitly, temporarily add a `System.out.println` or use a debugger breakpoint inside `PromptBuilder.buildSchedulePrompt` to view the prompt. You should see an "Examples to imitate" section containing text from the matched JSON entries.
8. Testing fallback: Clear the routine description or delete all entries in `routines.json` temporarily, rerun, and generate a schedule. The prompt should omit the example section, but generation (or fallback) should still run.
9. Adding a new example: Append a new JSON entry to `routines.json` (e.g., with keyword "triathlete"). Save, rerun the app, set **Routine Description** to include "triathlete" and generate again. You should see morning swim/run blocks (or other traits you defined) reflected in the new schedule after the next generation.

## 4. Test the solver (what you are testing)
Ensure the solver preserves fixed events, avoids overlaps, and reports unplaced activities when the schedule is overloaded.

1. Normal fit scenario:
    1. Use routine description: "Morning person, 9-5 job, light evenings".
    2. Fixed activities to add (click **Add** for each): "Mon 09:00-10:00 Team Sync", "Tue 11:00-12:00 1:1", "Thu 15:00-16:00 Demo".
    3. Generate the schedule.
    4. Expected: Fixed items show locked in their exact slots; other activities fill remaining gaps without overlaps. The grid should show distinct blocks with no double-stacked colors in the same hour/day.
2. Overloaded scenario:
    1. Use routine description: "Night owl, wants daily 3-hour study blocks".
    2. Add many fixed activities that fill most hours (e.g., every weekday 09:00-17:00 labeled "Workshop" by entering and clicking **Add** repeatedly for each day/time).
    3. Generate the schedule.
    4. Expected: Some proposed activities cannot be placed. The UI should either leave gaps for unplaced items or show a dialog/warning about unplaced activities; logs may mention `addUnplacedActivity`. Fixed items remain locked and unchanged.
3. (Optional) To force placement issues, add overlapping fixed entries (e.g., two events with the same day/time) and observe that only one can be honored while others become unplaced or trigger warnings depending on current logic.

## 5. Test the UI flow (what you are testing)
Confirm the end-to-end user interactions: view switching, generation trigger, locking, and validation dialogs.

1. Launch the app and click **Week** and **Day** buttons to see the layout switch between 7-day and single-day views.
2. Enter a routine description (e.g., "Balanced schedule, likes afternoon exercise") and fixed activities (e.g., "Sat 10:00-11:00 Yoga"), adding each with **Add**.
3. Click **Generate Schedule**. Observe the grid repaint: colored blocks appear for activities, gray blocks for blocked time, and lock icons for fixed/locked events.
4. Click a lock icon on any activity to toggle it, then use any provided **Regenerate** or **Generate Schedule** control to re-run; the locked item should stay in place while others move.
5. Validation: Try generating with an empty routine description to confirm a dialog appears requesting input. If generation fails for other reasons, expect a dialog noting that scheduling could not be completed.

## 6. Test without calling the AI (mock mode) (what you are testing)
Run the pipeline without hitting the real LLM to validate solver, presenter, and UI behavior.

1. No-API-key fallback:
    1. Remove `HUGGINGFACE_API_KEY` from your IntelliJ run configuration (or leave it blank).
    2. Run the app, enter a routine and fixed activities as usual, and click **Generate Schedule**.
    3. Expected: A deterministic, non-network fallback schedule appears (still colored blocks, locked fixed items). Use this to validate solver/UI without external calls.
2. Temporary stub LLM service:
    1. Open `Main` and locate where the real `LlmScheduleService` is constructed.
    2. Comment out that construction and replace it with a simple stub that returns a fixed list of proposed events (e.g., two activities at known times) without contacting any API.
    3. Re-run the app. Expected: The calendar shows exactly the stubbed activities placed by the solver; this lets you test the presenter and UI rendering in a fully deterministic way.
3. Solver-only checks:
    1. In a unit test or small harness, call `ConstraintSolver.solve` directly with a handcrafted list of proposed events and fixed events.
    2. Assert that non-overlapping events are placed, overlapping ones are moved or marked unplaced, and that `addUnplacedActivity` is invoked when space runs out.
4. UI-only rendering:
    1. Launch the app and (if available) trigger any built-in demo/fallback schedule or reuse the stubbed LLM output above.
    2. Verify that the grid paints blocks, locks display correctly, and day/week toggles continue to work—all without any network interaction.