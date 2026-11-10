package com.batyanko.strokeratecoach.workout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class WorkoutJsonCodec {
    private static final int SCHEMA_VERSION = 1;
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?(0|[1-9][0-9]*)");
    private static final Set<String> ROOT_FIELDS = new HashSet<>(
            Arrays.asList("schemaVersion", "nodes", "skipLastRest"));
    private static final Set<String> PHASE_FIELDS = new HashSet<>(
            Arrays.asList("type", "length", "targetSpm", "label"));
    private static final Set<String> REPEAT_FIELDS = new HashSet<>(
            Arrays.asList("type", "count", "label", "children"));

    public String serialize(WorkoutDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }

        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("skipLastRest", definition.shouldSkipLastRest());
        JsonArray nodes = new JsonArray();
        for (WorkoutNode node : definition.getNodes()) {
            nodes.add(serializeNode(node));
        }
        root.add("nodes", nodes);
        return root.toString();
    }

    public List<WorkoutNode> deserializeNodes(String json) {
        return parseTree(json).nodes;
    }

    public WorkoutDefinition deserializeDefinition(String json, WorkoutUnit unit) {
        ParsedTree tree = parseTree(json);
        return new WorkoutDefinition(unit, tree.nodes, tree.skipLastRest);
    }

    private ParsedTree parseTree(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw error(WorkoutValidationException.Code.MALFORMED_JSON,
                    "Workout JSON must not be empty");
        }

        JsonElement parsed = parseDocument(json);
        if (!parsed.isJsonObject()) {
            throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                    "Workout JSON root must be an object");
        }

        JsonObject root = parsed.getAsJsonObject();
        requireOnlyFields(root, ROOT_FIELDS, "workout");
        int version = requiredInteger(root, "schemaVersion", "workout");
        if (version != SCHEMA_VERSION) {
            throw error(WorkoutValidationException.Code.UNSUPPORTED_SCHEMA_VERSION,
                    "Unsupported workout schema version: " + version);
        }

        JsonArray nodes = requiredArray(root, "nodes", "workout");
        return new ParsedTree(
                parseNodes(nodes, "nodes", 0),
                optionalBoolean(root, "skipLastRest", false, "workout"));
    }

    private JsonElement parseDocument(String json) {
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setLenient(false);
            JsonElement parsed = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw error(WorkoutValidationException.Code.MALFORMED_JSON,
                        "Unexpected content after workout JSON");
            }
            return parsed;
        } catch (WorkoutValidationException e) {
            throw e;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.MALFORMED_JSON,
                    "Malformed workout JSON", e);
        }
    }

    private JsonObject serializeNode(WorkoutNode node) {
        if (node instanceof PhaseNode) {
            return serializePhase((PhaseNode) node);
        }
        if (node instanceof RepeatNode) {
            return serializeRepeat((RepeatNode) node);
        }
        throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                "Unsupported workout node implementation");
    }

    private JsonObject serializePhase(PhaseNode phase) {
        JsonObject object = new JsonObject();
        object.addProperty("type", "phase");
        object.addProperty("length", phase.getLength());
        object.addProperty("targetSpm", phase.getTargetSpm());
        addLabel(object, phase.getLabel());
        return object;
    }

    private JsonObject serializeRepeat(RepeatNode repeat) {
        JsonObject object = new JsonObject();
        object.addProperty("type", "repeat");
        object.addProperty("count", repeat.getRepeatCount());
        addLabel(object, repeat.getLabel());

        JsonArray children = new JsonArray();
        for (WorkoutNode child : repeat.getChildren()) {
            children.add(serializeNode(child));
        }
        object.add("children", children);
        return object;
    }

    private void addLabel(JsonObject object, String label) {
        if (label == null) {
            return;
        }
        object.addProperty("label", label);
    }

    private List<WorkoutNode> parseNodes(JsonArray array, String path, int repeatDepth) {
        List<WorkoutNode> nodes = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            String nodePath = path + "[" + i + "]";
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                        nodePath + " must be an object");
            }
            nodes.add(parseNode(element.getAsJsonObject(), nodePath, repeatDepth));
        }
        return nodes;
    }

    private WorkoutNode parseNode(JsonObject object, String path, int repeatDepth) {
        String type = requiredString(object, "type", path);
        if ("phase".equals(type)) {
            requireOnlyFields(object, PHASE_FIELDS, path);
            return parsePhase(object, path);
        }
        if ("repeat".equals(type)) {
            requireOnlyFields(object, REPEAT_FIELDS, path);
            return parseRepeat(object, path, repeatDepth + 1);
        }
        throw error(WorkoutValidationException.Code.UNKNOWN_NODE_TYPE,
                path + " has unknown node type: " + type);
    }

    private PhaseNode parsePhase(JsonObject object, String path) {
        return new PhaseNode(
                requiredInteger(object, "length", path),
                requiredInteger(object, "targetSpm", path),
                optionalLabel(object, path));
    }

    private RepeatNode parseRepeat(JsonObject object, String path, int repeatDepth) {
        if (repeatDepth > WorkoutCompiler.MAX_REPEAT_DEPTH) {
            throw error(WorkoutValidationException.Code.MAX_REPEAT_DEPTH_EXCEEDED,
                    path + " exceeds maximum repeat nesting depth of "
                            + WorkoutCompiler.MAX_REPEAT_DEPTH);
        }
        JsonArray children = requiredArray(object, "children", path);
        return new RepeatNode(
                requiredInteger(object, "count", path),
                optionalLabel(object, path),
                parseNodes(children, path + ".children", repeatDepth));
    }

    private String requiredString(JsonObject object, String field, String path) {
        JsonElement element = required(object, field, path);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                    path + "." + field + " must be a string");
        }
        return element.getAsString();
    }

    private String optionalLabel(JsonObject object, String path) {
        if (!object.has("label")) {
            return null;
        }
        return requiredString(object, "label", path);
    }

    private boolean optionalBoolean(JsonObject object, String field, boolean defaultValue,
                                    String path) {
        if (!object.has(field)) {
            return defaultValue;
        }
        JsonElement element = required(object, field, path);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                    path + "." + field + " must be a boolean");
        }
        return element.getAsBoolean();
    }

    private JsonArray requiredArray(JsonObject object, String field, String path) {
        JsonElement element = required(object, field, path);
        if (!element.isJsonArray()) {
            throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                    path + "." + field + " must be an array");
        }
        return element.getAsJsonArray();
    }

    private int requiredInteger(JsonObject object, String field, String path) {
        JsonElement element = required(object, field, path);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                    path + "." + field + " must be an integer");
        }

        String number = element.getAsString();
        if (!INTEGER_PATTERN.matcher(number).matches()) {
            throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                    path + "." + field + " must be an integer");
        }
        try {
            return new BigDecimal(number).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                    path + "." + field + " must be an integer in the Java int range", e);
        }
    }

    private JsonElement required(JsonObject object, String field, String path) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            throw error(WorkoutValidationException.Code.MISSING_FIELD,
                    path + " is missing required field '" + field + "'");
        }
        return object.get(field);
    }

    private void requireOnlyFields(JsonObject object, Set<String> allowedFields, String path) {
        for (String field : object.keySet()) {
            if (allowedFields.contains(field)) {
                continue;
            }
            throw error(WorkoutValidationException.Code.INVALID_FIELD_TYPE,
                    path + " contains unsupported field '" + field + "'");
        }
    }

    private WorkoutValidationException error(WorkoutValidationException.Code code, String message) {
        return new WorkoutValidationException(code, message);
    }

    private static final class ParsedTree {
        private final List<WorkoutNode> nodes;
        private final boolean skipLastRest;

        private ParsedTree(List<WorkoutNode> nodes, boolean skipLastRest) {
            this.nodes = nodes;
            this.skipLastRest = skipLastRest;
        }
    }
}
