/*
 * Copyright 2021 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
