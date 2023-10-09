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

package com.leinardi.pycharm.mypy.mpapi;

import java.util.Objects;

/**
 * An issue as reported by the Mypy tool.
 */

public class Issue {

    private final String path;
    private final int line;
    private final int column;
    private final SeverityLevel severityLevel;
    private final String message;

    public Issue(String path, int line, int column, SeverityLevel severityLevel, String message) {
        this.path = path;
        this.line = line;
        this.column = column;
        this.severityLevel = severityLevel;
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public SeverityLevel getSeverityLevel() {
        return severityLevel;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "path='" + path + '\'' +
                ", line=" + line +
                ", column=" + column +
                ", severityLevel=" + severityLevel +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Issue)) {
            return false;
        }
        Issue issue = (Issue) o;
        return line == issue.line &&
                column == issue.column &&
                Objects.equals(path, issue.path) &&
                severityLevel == issue.severityLevel &&
                Objects.equals(message, issue.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, line, column, severityLevel, message);
    }
}
