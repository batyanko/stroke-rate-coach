package com.batyanko.strokeratecoach.workout;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public final class WorkoutBackupCodec {
    private static final String NAME_PREFIX = "Name: ";
    private static final String DESCRIPTION_PREFIX = "Desc: ";
    private static final String TYPE_PREFIX = "Type: ";
    private static final String SPM_PREFIX = "SPM: ";
    private static final String SPP_PREFIX = "SPP: ";
    private static final String TREE_PREFIX = "TREE_B64:";

    private final WorkoutJsonCodec jsonCodec;
    private final LegacyWorkoutParser legacyParser;
    private final WorkoutCompiler compiler;

    public WorkoutBackupCodec(WorkoutJsonCodec jsonCodec,
                              LegacyWorkoutParser legacyParser,
                              WorkoutCompiler compiler) {
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
        this.legacyParser = Objects.requireNonNull(legacyParser, "legacyParser");
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public String encodeRecord(String name, String description, WorkoutDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        CompiledWorkout compiled = compiler.compile(definition);
        String encodedTree = Base64.getEncoder().encodeToString(
                jsonCodec.serialize(definition).getBytes(StandardCharsets.UTF_8));

        StringBuilder record = new StringBuilder()
                .append(NAME_PREFIX).append(nullToEmpty(name)).append('\n')
                .append(DESCRIPTION_PREFIX).append(nullToEmpty(description)).append('\n')
                .append(TYPE_PREFIX).append(definition.getUnit().dbValue()).append('\n')
                .append(SPM_PREFIX).append(joinTargets(compiled)).append('\n')
                .append(SPP_PREFIX).append(joinLengths(compiled)).append('\n')
                .append(TREE_PREFIX).append(' ').append(encodedTree);
        return record.toString();
    }

    public BackupRecord parseRecord(String text, String fallbackName) {
        Objects.requireNonNull(text, "text");
        String name = fallbackName;
        String description = null;
        String unit = null;
        String spm = null;
        String spp = null;
        String encodedTree = null;
        boolean treeFieldPresent = false;
        boolean nameFound = false;
        boolean descriptionFound = false;
        boolean typeFound = false;
        boolean spmFound = false;
        boolean sppFound = false;

        for (String line : text.split("\n", -1)) {
            if (line.startsWith(TREE_PREFIX)) {
                if (treeFieldPresent) {
                    throw invalidBackup("Backup record contains duplicate TREE_B64 fields");
                }
                treeFieldPresent = true;
                String value = line.substring(TREE_PREFIX.length());
                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                if (value.endsWith("\r")) {
                    value = value.substring(0, value.length() - 1);
                }
                encodedTree = value;
                continue;
            }

            String detail = line.trim();
            if (detail.startsWith(NAME_PREFIX)) {
                ensureUnique(nameFound, "Name");
                nameFound = true;
                name = detail.substring(NAME_PREFIX.length());
                continue;
            }
            if (detail.startsWith(DESCRIPTION_PREFIX)) {
                ensureUnique(descriptionFound, "Desc");
                descriptionFound = true;
                description = detail.substring(DESCRIPTION_PREFIX.length());
                continue;
            }
            if (detail.startsWith(TYPE_PREFIX)) {
                ensureUnique(typeFound, "Type");
                typeFound = true;
                unit = detail.substring(TYPE_PREFIX.length());
                continue;
            }
            if (detail.startsWith(SPM_PREFIX)) {
                ensureUnique(spmFound, "SPM");
                spmFound = true;
                spm = detail.substring(SPM_PREFIX.length());
                continue;
            }
            if (detail.startsWith(SPP_PREFIX)) {
                ensureUnique(sppFound, "SPP");
                sppFound = true;
                spp = detail.substring(SPP_PREFIX.length());
            }
        }

        if (!typeFound || !spmFound || !sppFound) {
            throw invalidBackup("Backup record must include Type, SPM, and SPP fields");
        }
        return new BackupRecord(name, description, unit, spm, spp,
                treeFieldPresent, encodedTree);
    }

    private WorkoutDefinition definitionFor(BackupRecord record) {
        if (!record.hasTreeField) {
            WorkoutDefinition legacy = legacyParser.parse(record.spp, record.spm, record.unit);
            compiler.compile(legacy);
            return legacy;
        }

        byte[] jsonBytes;
        try {
            jsonBytes = Base64.getDecoder().decode(record.encodedTree);
        } catch (IllegalArgumentException exception) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.INVALID_BACKUP_TREE,
                    "TREE_B64 is not valid standard Base64", exception);
        }

        String json = decodeUtf8(jsonBytes);
        WorkoutDefinition definition = jsonCodec.deserializeDefinition(
                json, WorkoutUnit.fromDbValue(record.unit));
        CompiledWorkout compiled = compiler.compile(definition);
        if (!joinLengths(compiled).equals(record.spp)
                || !joinTargets(compiled).equals(record.spm)) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.TREE_CSV_MISMATCH,
                    "TREE_B64 phase projection does not match SPP and SPM fields");
        }
        return definition;
    }

    private String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new WorkoutValidationException(
                    WorkoutValidationException.Code.INVALID_BACKUP_TREE,
                    "TREE_B64 does not contain valid UTF-8", exception);
        }
    }

    private String joinLengths(CompiledWorkout workout) {
        StringBuilder values = new StringBuilder();
        for (CompiledPhase phase : workout.getPhases()) {
            appendValue(values, phase.getLength());
        }
        return values.toString();
    }

    private String joinTargets(CompiledWorkout workout) {
        StringBuilder values = new StringBuilder();
        for (CompiledPhase phase : workout.getPhases()) {
            appendValue(values, phase.getTargetSpm());
        }
        return values.toString();
    }

    private void appendValue(StringBuilder values, int value) {
        if (values.length() > 0) {
            values.append(',');
        }
        values.append(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void ensureUnique(boolean found, String field) {
        if (found) {
            throw invalidBackup("Backup record contains duplicate " + field + " fields");
        }
    }

    private WorkoutValidationException invalidBackup(String message) {
        return new WorkoutValidationException(
                WorkoutValidationException.Code.INVALID_BACKUP_RECORD, message);
    }

    public final class BackupRecord {
        private final String name;
        private final String description;
        private final String unit;
        private final String spm;
        private final String spp;
        private final boolean hasTreeField;
        private final String encodedTree;
        private BackupRecord(String name, String description, String unit, String spm, String spp,
                             boolean hasTreeField, String encodedTree) {
            this.name = name;
            this.description = description;
            this.unit = unit;
            this.spm = spm;
            this.spp = spp;
            this.hasTreeField = hasTreeField;
            this.encodedTree = encodedTree;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public WorkoutDefinition getDefinition() {
            return definitionFor(this);
        }
    }
}
