package com.batyanko.strokeratecoach.workout;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WorkoutEditorDraft {
    private static final String PHASE = "phase";
    private static final String REPEAT = "repeat";

    private final List<Node> nodes;
    private final List<Integer> path;

    public WorkoutEditorDraft() {
        nodes = new ArrayList<>();
        path = new ArrayList<>();
    }

    public List<Node> getCurrentNodes() {
        List<Node> current = nodes;
        for (Integer childIndex : path) {
            Node parent = current.get(childIndex);
            current = parent.children;
        }
        return current;
    }

    public List<Node> getRootNodes() {
        return nodes;
    }

    public List<Integer> getPath() {
        return Collections.unmodifiableList(new ArrayList<>(path));
    }

    public void enterRepeat(int index) {
        Node selected = getCurrentNodes().get(index);
        if (!selected.isRepeat()) {
            throw new IllegalArgumentException("Only repeat nodes can be opened");
        }
        path.add(index);
    }

    public void addRepeat(int repeatCount, String label) {
        List<Node> current = getCurrentNodes();
        current.add(Node.repeat(repeatCount, label));
        path.add(current.size() - 1);
    }

    public void goUp() {
        if (!path.isEmpty()) {
            path.remove(path.size() - 1);
        }
    }

    public WorkoutDefinition toDefinition(WorkoutUnit unit) {
        return new WorkoutDefinition(unit, toWorkoutNodes(nodes));
    }

    public String serialize() {
        return new Gson().toJson(this);
    }

    public static WorkoutEditorDraft deserialize(String json) {
        WorkoutEditorDraft draft;
        try {
            draft = new Gson().fromJson(json, WorkoutEditorDraft.class);
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException(
                    "Saved workout editor state is malformed", exception);
        }
        if (draft == null || draft.nodes == null || draft.path == null) {
            throw new IllegalArgumentException("Saved workout editor state is incomplete");
        }
        draft.validateNodes(draft.nodes);
        draft.validatePath();
        return draft;
    }

    public static WorkoutEditorDraft fromDefinition(WorkoutDefinition definition) {
        WorkoutEditorDraft draft = new WorkoutEditorDraft();
        draft.nodes.addAll(fromWorkoutNodes(definition.getNodes()));
        return draft;
    }

    private void validatePath() {
        List<Node> current = nodes;
        for (Integer childIndex : path) {
            if (childIndex == null || childIndex < 0 || childIndex >= current.size()) {
                throw new IllegalArgumentException("Saved workout editor location is invalid");
            }
            Node selected = current.get(childIndex);
            if (selected == null || !selected.isRepeat() || selected.children == null) {
                throw new IllegalArgumentException("Saved workout editor location is invalid");
            }
            current = selected.children;
        }
    }

    private void validateNodes(List<Node> items) {
        for (Node node : items) {
            if (node == null
                    || (!PHASE.equals(node.type) && !REPEAT.equals(node.type))) {
                throw new IllegalArgumentException("Saved workout editor node is invalid");
            }
            if (node.isRepeat()) {
                if (node.children == null) {
                    throw new IllegalArgumentException("Saved repeat children are invalid");
                }
                validateNodes(node.children);
            }
        }
    }

    private static List<WorkoutNode> toWorkoutNodes(List<Node> draftNodes) {
        List<WorkoutNode> result = new ArrayList<>(draftNodes.size());
        for (Node draftNode : draftNodes) {
            if (draftNode.isRepeat()) {
                result.add(new RepeatNode(draftNode.repeatCount, draftNode.label,
                        toWorkoutNodes(draftNode.children)));
                continue;
            }
            result.add(new PhaseNode(draftNode.length, draftNode.targetSpm));
        }
        return result;
    }

    private static List<Node> fromWorkoutNodes(List<WorkoutNode> workoutNodes) {
        List<Node> result = new ArrayList<>(workoutNodes.size());
        for (WorkoutNode workoutNode : workoutNodes) {
            if (workoutNode instanceof RepeatNode) {
                RepeatNode repeat = (RepeatNode) workoutNode;
                Node draftRepeat = Node.repeat(repeat.getRepeatCount(), repeat.getLabel());
                draftRepeat.children.addAll(fromWorkoutNodes(repeat.getChildren()));
                result.add(draftRepeat);
                continue;
            }
            PhaseNode phase = (PhaseNode) workoutNode;
            result.add(Node.phase(phase.getLength(), phase.getTargetSpm()));
        }
        return result;
    }

    public static final class Node {
        private String type;
        private int length;
        private int targetSpm;
        private int repeatCount;
        private String label;
        private List<Node> children = new ArrayList<>();

        private Node() {
        }

        public static Node phase(int length, int targetSpm) {
            Node node = new Node();
            node.type = PHASE;
            node.length = length;
            node.targetSpm = targetSpm;
            return node;
        }

        public static Node repeat(int repeatCount, String label) {
            Node node = new Node();
            node.type = REPEAT;
            node.repeatCount = repeatCount;
            node.label = label;
            return node;
        }

        public boolean isRepeat() {
            return REPEAT.equals(type);
        }

        public int getLength() {
            return length;
        }

        public int getTargetSpm() {
            return targetSpm;
        }

        public int getRepeatCount() {
            return repeatCount;
        }

        public String getLabel() {
            return label;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void updatePhase(int updatedLength, int updatedTargetSpm) {
            length = updatedLength;
            targetSpm = updatedTargetSpm;
        }

        public void updateRepeat(int updatedRepeatCount, String updatedLabel) {
            repeatCount = updatedRepeatCount;
            label = updatedLabel;
        }
    }
}
