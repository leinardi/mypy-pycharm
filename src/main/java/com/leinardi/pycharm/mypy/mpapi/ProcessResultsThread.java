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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.util.ThrowableRunnable;
import com.leinardi.pycharm.mypy.checker.Problem;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessResultsThread implements ThrowableRunnable<RuntimeException> {

    private static final Logger LOG = Logger.getInstance(ProcessResultsThread.class);

    private final boolean suppressErrors;
    private final int tabWidth;
    private final String baseDir;
    private final List<Issue> errors;
    private final Map<String, PsiFile> fileNamesToPsiFiles;

    private final Map<PsiFile, List<Problem>> problems = new HashMap<>();

    private static final class Position {
        private final boolean afterEndOfLine;
        private final int offset;

        public static Position at(final int offset, final boolean afterEndOfLine) {
            return new Position(offset, afterEndOfLine);
        }

        public static Position at(final int offset) {
            return new Position(offset, false);
        }

        private Position(final int offset, final boolean afterEndOfLine) {
            this.offset = offset;
            this.afterEndOfLine = afterEndOfLine;
        }

        private PsiElement element(final PsiFile psiFile) {
            return psiFile.findElementAt(offset);
        }
    }

    public ProcessResultsThread(final boolean suppressErrors,
                                final int tabWidth,
                                final String baseDir,
                                final List<Issue> errors,
                                final Map<String, PsiFile> fileNamesToPsiFiles) {
        this.suppressErrors = suppressErrors;
        this.tabWidth = tabWidth;
        this.baseDir = baseDir;
        this.errors = errors;
        this.fileNamesToPsiFiles = fileNamesToPsiFiles;
    }

    @Override
    public void run() {
        final Map<PsiFile, List<Integer>> lineLengthCachesByFile = new HashMap<>();

        for (final Issue event : errors) {
            final PsiFile psiFile = fileNamesToPsiFiles.get(filenameFrom(event));
            if (psiFile == null) {
                LOG.info("Could not find mapping for file: " + event.getPath() + " in " + fileNamesToPsiFiles);
                return;
            }

            List<Integer> lineLengthCache = lineLengthCachesByFile.get(psiFile);
            if (lineLengthCache == null) {
                // we cache the offset of each line as it is created, so as to
                // avoid retreating ground we've already covered.
                lineLengthCache = new ArrayList<>();
                lineLengthCache.add(0); // line 1 is offset 0

                lineLengthCachesByFile.put(psiFile, lineLengthCache);
            }

            processEvent(psiFile, lineLengthCache, event);
        }
    }

    private String filenameFrom(final Issue issue) {
        String path = normalisePath(withTrailingSeparator(baseDir) + issue.getPath());
        if (new File(path).exists()) {
            return path;
        } else {
            return normalisePath(issue.getPath());
        }
    }

    private String normalisePath(String prefixedFileName) {
        try {
            return Paths.get(prefixedFileName).normalize().toString();
        } catch (InvalidPathException e) {
            return prefixedFileName;  // cannot normalize
        }
    }

    private String withTrailingSeparator(final String path) {
        if (path != null && !path.endsWith(File.separator)) {
            return path + File.separator;
        }
        return path;
    }

    private void processEvent(final PsiFile psiFile, final List<Integer> lineLengthCache, final Issue event) {
        //        if (additionalChecksFail(psiFile, event)) {
        //            return;
        //        }

        final Position position = findPosition(lineLengthCache, event, psiFile.textToCharArray());
        final PsiElement victim = position.element(psiFile);

        if (victim != null) {
            addProblemTo(victim, psiFile, event, position.afterEndOfLine);
        } else {
            addProblemTo(psiFile, psiFile, event, false);
            LOG.debug("Couldn't find victim for error: " + event.getPath() + "(" + event.getLine() + ":"
                    + event.getColumn() + ") " + event.getMessage());
        }
    }

    private void addProblemTo(final PsiElement victim,
                              final PsiFile psiFile,
                              @NotNull final Issue issue,
                              final boolean afterEndOfLine) {
        try {
            addProblem(psiFile,
                    new Problem(
                            victim,
                            issue.getMessage(),
                            issue.getSeverityLevel(),
                            issue.getLine(),
                            issue.getColumn(),
                            afterEndOfLine,
                            suppressErrors));
        } catch (PsiInvalidElementAccessException e) {
            LOG.warn("Element access failed", e);
        }
    }

    //    private boolean additionalChecksFail(final PsiFile psiFile, final Issue event) {
    //        for (final Check check : checks) {
    //            if (!check.process(psiFile, event.sourceName)) {
    //                return true;
    //            }
    //        }
    //        return false;
    //    }

    @NotNull
    private Position findPosition(final List<Integer> lineLengthCache, final Issue event, final char[] text) {
        if (event.getLine() == 0) {
            return Position.at(event.getColumn());
        } else if (event.getLine() <= lineLengthCache.size()) {
            return Position.at(lineLengthCache.get(event.getLine() - 1) + event.getColumn());
        } else {
            return searchFromEndOfCachedData(lineLengthCache, event, text);
        }
    }

    @NotNull
    private Position searchFromEndOfCachedData(final List<Integer> lineLengthCache,
                                               final Issue event,
                                               final char[] text) {
        final Position position;
        int offset = lineLengthCache.get(lineLengthCache.size() - 1);
        boolean afterEndOfLine = false;
        int line = lineLengthCache.size();

        int column = 0;
        for (int i = offset; i < text.length; ++i) {
            final char character = text[i];

            // for linefeeds we need to handle CR, LF and CRLF,
            // hence we accept either and only trigger a new
            // line on the LF of CRLF.
            final char nextChar = nextCharacter(text, i);
            if (character == '\n' || (character == '\r' && nextChar != '\n')) {
                ++line;
                ++offset;
                lineLengthCache.add(offset);
                column = 0;
            } else if (character == '\t') {
                column += tabWidth;
                ++offset;
            } else {
                ++column;
                ++offset;
            }

            if (event.getLine() == line && event.getColumn() == column) {
                if (column == 0 && Character.isWhitespace(nextChar)) {
                    afterEndOfLine = true;
                }
                break;
            }
        }

        position = Position.at(offset, afterEndOfLine);
        return position;
    }

    private char nextCharacter(final char[] text, final int i) {
        if ((i + 1) < text.length) {
            return text[i + 1];
        }
        return '\0';
    }

    @NotNull
    public Map<PsiFile, List<Problem>> getProblems() {
        return Collections.unmodifiableMap(problems);
    }

    private void addProblem(final PsiFile psiFile, final Problem problem) {
        List<Problem> problemsForFile = problems.get(psiFile);
        if (problemsForFile == null) {
            problemsForFile = new ArrayList<>();
            problems.put(psiFile, problemsForFile);
        }

        problemsForFile.add(problem);
    }
}
