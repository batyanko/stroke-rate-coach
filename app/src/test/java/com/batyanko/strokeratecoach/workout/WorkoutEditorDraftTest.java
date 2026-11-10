package com.batyanko.strokeratecoach.workout;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WorkoutEditorDraftTest {
    @Test
    public void preservesEmptyRepeatAndNavigationLocationAcrossSerialization() {
        WorkoutEditorDraft draft = new WorkoutEditorDraft();
        draft.addRepeat(3, "Set");
        draft.addRepeat(2, "Empty nested set");

        WorkoutEditorDraft restored = WorkoutEditorDraft.deserialize(draft.serialize());

        assertEquals(2, restored.getPath().size());
        assertEquals(0, restored.getCurrentNodes().size());
        assertEquals(1, restored.getRootNodes().get(0).getChildren().size());
        assertEquals(0, restored.getRootNodes().get(0).getChildren().get(0)
                .getChildren().size());
    }

    @Test
    public void preservesNestedTreeOrderAndLabelsWhenEditingExistingWorkout() {
        WorkoutDefinition definition = new WorkoutDefinition(WorkoutUnit.SECONDS,
                java.util.Arrays.asList(
                        new PhaseNode(30, 20),
                        new RepeatNode(2, "Set",
                                java.util.Arrays.asList(
                                        new PhaseNode(60, 22),
                                        new RepeatNode(3, "Sprint",
                                                java.util.Collections.singletonList(
                                                        new PhaseNode(10, 30)))))));

        WorkoutEditorDraft draft = WorkoutEditorDraft.fromDefinition(definition);
        WorkoutDefinition restored = WorkoutEditorDraft.deserialize(draft.serialize())
                .toDefinition(WorkoutUnit.SECONDS);
        CompiledWorkout compiled = new WorkoutCompiler().compile(restored);

        assertEquals(9, compiled.getPhases().size());
        assertEquals("Set 1/2 · Sprint 1/3",
                compiled.getPhases().get(2).getBreadcrumb());
        assertEquals(30, compiled.getPhases().get(0).getLength());
        assertEquals(60, compiled.getPhases().get(1).getLength());
    }

    @Test
    public void preservesSkipLastRestWhenSerializingAndEditingDraft() {
        WorkoutDefinition definition = new WorkoutDefinition(WorkoutUnit.SECONDS,
                java.util.Collections.singletonList(new RepeatNode(2,
                        java.util.Collections.singletonList(new PhaseNode(10, 0)))),
                true);

        WorkoutEditorDraft restored = WorkoutEditorDraft.deserialize(
                WorkoutEditorDraft.fromDefinition(definition).serialize());
        WorkoutDefinition restoredDefinition = restored.toDefinition(WorkoutUnit.SECONDS);
        CompiledWorkout compiled = new WorkoutCompiler().compile(restoredDefinition);

        assertEquals(true, restoredDefinition.shouldSkipLastRest());
        assertEquals(10, compiled.getPhases().get(0).getLength());
        assertEquals(1, compiled.getTotalPhases());
    }

    @Test
    public void compilerRejectsIncompleteDraftAfterConversion() {
        WorkoutEditorDraft draft = new WorkoutEditorDraft();
        draft.addRepeat(2, "Empty set");

        try {
            new WorkoutCompiler().compile(draft.toDefinition(WorkoutUnit.STROKES));
        } catch (WorkoutValidationException exception) {
            assertEquals(WorkoutValidationException.Code.EMPTY_REPEAT, exception.getCode());
            return;
        }
        throw new AssertionError("Expected empty repeat validation to fail");
    }
}
