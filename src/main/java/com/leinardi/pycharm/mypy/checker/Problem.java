/*
 * Copyright 2023 Roberto Leinardi.
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

package com.leinardi.pycharm.mypy.checker;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.leinardi.pycharm.mypy.MypyBundle;
import com.leinardi.pycharm.mypy.intentions.TypeIgnoreIntention;
import com.leinardi.pycharm.mypy.mpapi.SeverityLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Problem {
    private final PsiElement target;
    private final SeverityLevel severityLevel;
    private final int line;
    private final int column;
    private final String message;
    private final boolean afterEndOfLine;
    private final boolean suppressErrors;

    public Problem(@NotNull final PsiElement target,
                   @NotNull final String message,
                   @NotNull final SeverityLevel severityLevel,
                   final int line,
                   final int column,
                   final boolean afterEndOfLine,
                   final boolean suppressErrors) {
        this.target = target;
        this.message = message;
        this.severityLevel = severityLevel;
        this.line = line;
        this.column = column;
        this.afterEndOfLine = afterEndOfLine;
        this.suppressErrors = suppressErrors;
    }

    public void createAnnotation(@NotNull AnnotationHolder holder, @NotNull HighlightSeverity severity) {
        String message = MypyBundle.message("inspection.message", getMessage());
        AnnotationBuilder annotation = holder
                .newAnnotation(severity, message)
                .range(target.getTextRange())
                .withFix(new TypeIgnoreIntention());
        if (isAfterEndOfLine()) {
            annotation = annotation.afterEndOfLine();
        }
        annotation.create();
    }

    public SeverityLevel severityLevel() {
        return severityLevel;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAfterEndOfLine() {
        return afterEndOfLine;
    }

    public boolean isSuppressErrors() {
        return suppressErrors;
    }

    @Override
    public String toString() {
        return "Problem{" +
                "target=" + target +
                ", severityLevel=" + severityLevel +
                ", line=" + line +
                ", column=" + column +
                ", message='" + message + '\'' +
                ", afterEndOfLine=" + afterEndOfLine +
                ", suppressErrors=" + suppressErrors +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Problem)) {
            return false;
        }
        Problem problem = (Problem) o;
        return line == problem.line &&
                column == problem.column &&
                afterEndOfLine == problem.afterEndOfLine &&
                suppressErrors == problem.suppressErrors &&
                Objects.equals(target, problem.target) &&
                severityLevel == problem.severityLevel &&
                Objects.equals(message, problem.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, severityLevel, line, column, message, afterEndOfLine, suppressErrors);
    }
}
