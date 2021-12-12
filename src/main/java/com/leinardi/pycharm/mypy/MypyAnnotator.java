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

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiFile;
import com.leinardi.pycharm.mypy.checker.Problem;
import com.leinardi.pycharm.mypy.checker.ScanFiles;
import com.leinardi.pycharm.mypy.checker.ScannableFile;
import com.leinardi.pycharm.mypy.exception.MypyPluginParseException;
import com.leinardi.pycharm.mypy.mpapi.MypyRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.leinardi.pycharm.mypy.MypyBundle.message;
import static com.leinardi.pycharm.mypy.util.Notifications.showException;
import static com.leinardi.pycharm.mypy.util.Notifications.showWarning;
import static java.util.Collections.singletonList;

/**
 * Using the `ExternalAnnotator` API instead of `LocalInspectionTool`, because the former has better behavior with
 * long-running expensive checkers like mypy. Following multiple successive changes to a file, `LocalInspectionTool`
 * can invoke the checker for each modification from multiple threads in parallel, which can bog down the system
 * (see https://github.com/leinardi/mypy-pycharm/issues/43).
 * `ExternalAnnotator` cancels the previous running check (if any) before running the next one.
 * <p>
 * Modeled after `com.jetbrains.python.validation.Pep8ExternalAnnotator`
 * <p>
 * IDE calls methods in three phases:
 * 1. `State collectInformation(PsiFile)`: preparation.
 * 2. `Results doAnnotate(State)`: called in the background.
 * 3. `void apply(PsiFile, Results, AnnotationHolder)`: apply annotations to the editor.
 */
public class MypyAnnotator extends ExternalAnnotator<MypyAnnotator.State, MypyAnnotator.Results> {
    /* Inner classes storing intermediate results */
    static class State {
        PsiFile file;

        public State(PsiFile file) {
            this.file = file;
        }
    }

    static class Results {
        List<Problem> issues;

        public Results(List<Problem> issues) {
            this.issues = issues;
        }
    }

    private static final Logger LOG = Logger.getInstance(MypyAnnotator.class);
    private static final Results NO_PROBLEMS_FOUND = new Results(Collections.emptyList());
    private static final String ERROR_MESSAGE_INVALID_SYNTAX = "invalid syntax";

    private MypyPlugin plugin(final Project project) {
        final MypyPlugin mypyPlugin = project.getService(MypyPlugin.class);
        if (mypyPlugin == null) {
            throw new IllegalStateException("Couldn't get mypy plugin");
        }
        return mypyPlugin;
    }

    /**
     * Integration with `MypyBatchInspection`
     */
    @Override
    public String getPairedBatchInspectionShortName() {
        return MypyBatchInspection.INSPECTION_SHORT_NAME;
    }

    @Nullable
    @Override
    public State collectInformation(@NotNull PsiFile file) {
        LOG.debug("Mypy collectInformation " + file.getName()
                + " modified=" + file.getModificationStamp()
                + " thread=" + Thread.currentThread().getName()
        );

        return new State(file);
    }

    @Nullable
    @Override
    public Results doAnnotate(State state) {
        PsiFile psiFile = state.file;
        Project project = psiFile.getProject();
        final MypyPlugin plugin = plugin(project);
        long startTime = System.currentTimeMillis();

        if (!MypyRunner.checkMypyAvailable(plugin.getProject())) {
            LOG.debug("Scan failed: Mypy not available.");
            return NO_PROBLEMS_FOUND;
        }

        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            scannableFiles.addAll(ScannableFile.createAndValidate(singletonList(psiFile), plugin));
            if (scannableFiles.isEmpty()) {
                return NO_PROBLEMS_FOUND;
            }
            ScanFiles scanFiles = new ScanFiles(plugin, Collections.singletonList(psiFile.getVirtualFile()));
            Map<PsiFile, List<Problem>> map = scanFiles.call();
            map.values().forEach(problems -> problems.removeIf(problem ->
                    problem.getMessage().equals(ERROR_MESSAGE_INVALID_SYNTAX)));
            if (map.isEmpty()) {
                return NO_PROBLEMS_FOUND;
            }

            long duration = System.currentTimeMillis() - startTime;
            LOG.debug("Mypy scan completed: " + psiFile.getName() + " in " + duration + " ms");
            return new Results(map.get(psiFile));

        } catch (ProcessCanceledException | AssertionError e) {
            LOG.debug("Process cancelled when scanning: " + psiFile.getName());
            return NO_PROBLEMS_FOUND;

        } catch (MypyPluginParseException e) {
            LOG.debug("Parse exception caught when scanning: " + psiFile.getName(), e);
            return NO_PROBLEMS_FOUND;

        } catch (Throwable e) {
            handlePluginException(e, psiFile, project);
            return NO_PROBLEMS_FOUND;

        } finally {
            scannableFiles.forEach(ScannableFile::deleteIfRequired);
        }
    }

    @Override
    public void apply(@NotNull PsiFile file, Results results, @NotNull AnnotationHolder holder) {
        if (results == null || !file.isValid()) {
            return;
        }

        LOG.debug("Applying " + results.issues.size() + " annotations for " + file.getName());

        // Get severity from inspection profile
        final InspectionProfile profile =
                InspectionProjectProfileManager.getInstance(file.getProject()).getCurrentProfile();
        final HighlightDisplayKey key = HighlightDisplayKey.find(MypyBatchInspection.INSPECTION_SHORT_NAME);
        HighlightSeverity severity = profile.getErrorLevel(key, file).getSeverity();

        for (Problem problem : results.issues) {
            LOG.debug("                " + problem.getLine() + ": " + problem.getMessage());
            problem.createAnnotation(holder, severity);
        }
    }

    private void handlePluginException(final Throwable e,
                                       final @NotNull PsiFile psiFile,
                                       final @NotNull Project project) {

        if (e.getCause() != null && e.getCause() instanceof ProcessCanceledException) {
            LOG.debug("Process cancelled when scanning: " + psiFile.getName());

        } else if (e.getCause() != null && e.getCause() instanceof IOException) {
            showWarning(project, message("mypy.file-io-failed"));

        } else {
            LOG.warn("Mypy threw an exception when scanning: " + psiFile.getName(), e);
            showException(project, e);
        }
    }
}
