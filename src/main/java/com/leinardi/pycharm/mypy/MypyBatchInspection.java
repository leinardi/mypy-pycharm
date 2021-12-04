package com.leinardi.pycharm.mypy;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection;
import org.jetbrains.annotations.NotNull;

/**
 * By itself, the `MypyAnnotator` class does not provide support for the explicit "Inspect code" feature.
 *
 * This class uses `ExternalAnnotatorBatchInspection` middleware to provides that functionality.
 *
 * Modeled after `com.jetbrains.python.inspections.PyPep8Inspection`
 */
public class MypyBatchInspection extends LocalInspectionTool implements ExternalAnnotatorBatchInspection {
    public static final String INSPECTION_SHORT_NAME = "Mypy";

    @Override
    public @NotNull String getShortName() {
        return INSPECTION_SHORT_NAME;
    }
}
