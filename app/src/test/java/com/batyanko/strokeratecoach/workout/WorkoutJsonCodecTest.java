package com.batyanko.strokeratecoach.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class WorkoutJsonCodecTest {
    private final WorkoutJsonCodec codec = new WorkoutJsonCodec();

    @Test
    public void roundTripPreservesOrderValuesCountsAndLabels() {
        WorkoutDefinition definition = new WorkoutDefinition(WorkoutUnit.SECONDS, Arrays.asList(
                new PhaseNode(12, 20, "Warmup"),
                new RepeatNode(2, "Set", Arrays.asList(
                        new PhaseNode(60, 22, "Work"),
                        new RepeatNode(3, Arrays.asList(new PhaseNode(10, 18)))))));

        String json = codec.serialize(definition);
        assertEquals(
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"phase\",\"length\":12,"
                        + "\"targetSpm\":20,\"label\":\"Warmup\"},{\"type\":\"repeat\","
                        + "\"count\":2,\"label\":\"Set\",\"children\":[{\"type\":\"phase\","
                        + "\"length\":60,\"targetSpm\":22,\"label\":\"Work\"},{\"type\":"
                        + "\"repeat\",\"count\":3,\"children\":[{\"type\":\"phase\","
                        + "\"length\":10,\"targetSpm\":18}]}]}]}",
                json);

        List<WorkoutNode> nodes = codec.deserializeNodes(json);
        assertEquals(2, nodes.size());
        assertPhase((PhaseNode) nodes.get(0), 12, 20, "Warmup");
        RepeatNode outer = (RepeatNode) nodes.get(1);
        assertEquals(2, outer.getRepeatCount());
        assertEquals("Set", outer.getLabel());
        assertPhase((PhaseNode) outer.getChildren().get(0), 60, 22, "Work");
        RepeatNode inner = (RepeatNode) outer.getChildren().get(1);
        assertEquals(3, inner.getRepeatCount());
        assertPhase((PhaseNode) inner.getChildren().get(0), 10, 18, null);
    }

    @Test
    public void rejectsMalformedAndTrailingJson() {
        assertCode(WorkoutValidationException.Code.MALFORMED_JSON, "{");
        assertCode(WorkoutValidationException.Code.MALFORMED_JSON,
                "{\"schemaVersion\":1,\"nodes\":[]} trailing");
        assertCode(WorkoutValidationException.Code.MALFORMED_JSON, "");
        assertCode(WorkoutValidationException.Code.MALFORMED_JSON, null);
    }

    @Test
    public void rejectsUnsupportedVersionsUnknownTypesAndMissingFields() {
        assertCode(WorkoutValidationException.Code.UNSUPPORTED_SCHEMA_VERSION,
                "{\"schemaVersion\":2,\"nodes\":[]}");
        assertCode(WorkoutValidationException.Code.UNKNOWN_NODE_TYPE,
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"other\"}]}");
        assertCode(WorkoutValidationException.Code.MISSING_FIELD,
                "{\"schemaVersion\":1}");
        assertCode(WorkoutValidationException.Code.MISSING_FIELD,
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"phase\",\"length\":2}]}");
        assertCode(WorkoutValidationException.Code.MISSING_FIELD,
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"repeat\",\"count\":2}]}");
    }

    @Test
    public void rejectsWrongTypesDecimalsAndOutOfRangeIntegers() {
        String[] invalidDocuments = {
                "{\"schemaVersion\":\"1\",\"nodes\":[]}",
                "{\"schemaVersion\":1.0,\"nodes\":[]}",
                "{\"schemaVersion\":1,\"nodes\":{}}",
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":true}]}",
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"phase\",\"length\":\"2\","
                        + "\"targetSpm\":22}]}",
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"phase\",\"length\":2.5,"
                        + "\"targetSpm\":22}]}",
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"phase\",\"length\":2,"
                        + "\"targetSpm\":2147483648}]}",
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"phase\",\"length\":2,"
                        + "\"targetSpm\":22,\"label\":false}]}",
                "{\"schemaVersion\":1,\"nodes\":[{\"type\":\"repeat\",\"count\":1.2,"
                        + "\"children\":[]}]}",
                "{\"schemaVersion\":1,\"nodes\":[],\"extra\":true}"
        };
        for (String document : invalidDocuments) {
            assertCode(WorkoutValidationException.Code.INVALID_FIELD_TYPE, document);
        }
    }

    @Test
    public void rejectsTreesDeeperThanTheCompilerLimitDuringParsing() {
        String nestedRepeats = "{\"type\":\"repeat\",\"count\":1,\"children\":[";
        String phase = "{\"type\":\"phase\",\"length\":1,\"targetSpm\":1}";
        StringBuilder node = new StringBuilder(phase);
        for (int i = 0; i < WorkoutCompiler.MAX_REPEAT_DEPTH + 1; i++) {
            node = new StringBuilder(nestedRepeats).append(node).append("]}");
        }
        assertCode(WorkoutValidationException.Code.MAX_REPEAT_DEPTH_EXCEEDED,
                "{\"schemaVersion\":1,\"nodes\":[" + node + "]}");
    }

    private void assertPhase(PhaseNode phase, int length, int targetSpm, String label) {
        assertEquals(length, phase.getLength());
        assertEquals(targetSpm, phase.getTargetSpm());
        assertEquals(label, phase.getLabel());
    }

    private void assertCode(WorkoutValidationException.Code code, String json) {
        try {
            codec.deserializeNodes(json);
            fail("Expected " + code);
        } catch (WorkoutValidationException exception) {
            assertNotNull(exception.getMessage());
            assertEquals(code, exception.getCode());
        }
    }
}
