/*
 * Copyright 2021 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.mypy.mpapi;

/**
 * An issue as reported by the Mypy tool.
 */

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

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
        return new ToStringBuilder(this)
                .append("path", path)
                .append("line", line)
                .append("column", column)
                .append("type", severityLevel)
                .append("message", message)
                .toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(path)
                .append(line)
                .append(column)
                .append(severityLevel)
                .append(message)
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Issue)) {
            return false;
        }
        Issue rhs = ((Issue) other);
        return new EqualsBuilder()
                .append(path, rhs.path)
                .append(line, rhs.line)
                .append(column, rhs.column)
                .append(severityLevel, rhs.severityLevel)
                .append(message, rhs.message)
                .isEquals();
    }

}
