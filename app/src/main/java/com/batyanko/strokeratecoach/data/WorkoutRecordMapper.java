package com.batyanko.strokeratecoach.data;

import android.content.ContentValues;
import android.database.Cursor;

import com.batyanko.strokeratecoach.data.WorkoutContract.WorkoutEntry1;
import com.batyanko.strokeratecoach.workout.CompiledPhase;
import com.batyanko.strokeratecoach.workout.CompiledWorkout;
import com.batyanko.strokeratecoach.workout.LegacyWorkoutParser;
import com.batyanko.strokeratecoach.workout.WorkoutCompiler;
import com.batyanko.strokeratecoach.workout.WorkoutDefinition;
import com.batyanko.strokeratecoach.workout.WorkoutJsonCodec;
import com.batyanko.strokeratecoach.workout.WorkoutNode;
import com.batyanko.strokeratecoach.workout.WorkoutUnit;

import java.util.List;
import java.util.Objects;

public final class WorkoutRecordMapper {
    private final WorkoutJsonCodec codec;
    private final LegacyWorkoutParser legacyParser;
    private final WorkoutCompiler compiler;

    public WorkoutRecordMapper(WorkoutJsonCodec codec,
                               LegacyWorkoutParser legacyParser,
                               WorkoutCompiler compiler) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.legacyParser = Objects.requireNonNull(legacyParser, "legacyParser");
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public WorkoutDefinition readDefinition(Cursor cursor) {
        Objects.requireNonNull(cursor, "cursor");

        String serializedTree = cursor.getString(
                cursor.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_SERIALIZED_TREE));
        if (serializedTree != null && !serializedTree.isEmpty()) {
            List<WorkoutNode> nodes = codec.deserializeNodes(serializedTree);
            WorkoutUnit unit = WorkoutUnit.fromDbValue(cursor.getString(
                    cursor.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_SPP_TYPE)));
            WorkoutDefinition definition = new WorkoutDefinition(unit, nodes);
            compiler.compile(definition);
            return definition;
        }

        return legacyParser.parse(
                cursor.getString(cursor.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_SPP_CSV)),
                cursor.getString(cursor.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_GEARS_CSV)),
                cursor.getString(cursor.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_SPP_TYPE)));
    }

    public CompiledWorkout readAndCompileDefinition(Cursor cursor) {
        return compiler.compile(readDefinition(cursor));
    }

    public void putDefinition(ContentValues values, WorkoutDefinition definition) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(definition, "definition");
        CompiledWorkout compiled = compiler.compile(definition);

        values.put(WorkoutEntry1.COLUMN_SPP_TYPE, definition.getUnit().dbValue());
        values.put(WorkoutEntry1.COLUMN_SERIALIZED_TREE, codec.serialize(definition));
        values.put(WorkoutEntry1.COLUMN_SPP_CSV, joinLengths(compiled));
        values.put(WorkoutEntry1.COLUMN_GEARS_CSV, joinTargets(compiled));
    }

    public void copyDefinition(Cursor source, ContentValues destination) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");

        String serializedTree = source.getString(
                source.getColumnIndexOrThrow(WorkoutEntry1.COLUMN_SERIALIZED_TREE));
        WorkoutDefinition definition = readDefinition(source);
        putDefinition(destination, definition);
        if (serializedTree != null && !serializedTree.isEmpty()) {
            destination.put(WorkoutEntry1.COLUMN_SERIALIZED_TREE, serializedTree);
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
}
