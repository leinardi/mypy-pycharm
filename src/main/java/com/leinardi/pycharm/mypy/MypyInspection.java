/*
 * Copyright 2018 Roberto Leinardi.
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

package com.leinardi.pycharm.mypy;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.leinardi.pycharm.mypy.checker.Problem;
import com.leinardi.pycharm.mypy.checker.ScanFiles;
import com.leinardi.pycharm.mypy.checker.ScannableFile;
import com.leinardi.pycharm.mypy.exception.MypyPluginParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.leinardi.pycharm.mypy.MypyBundle.message;
import static com.leinardi.pycharm.mypy.util.Async.asyncResultOf;
import static com.leinardi.pycharm.mypy.util.Notifications.showException;
import static com.leinardi.pycharm.mypy.util.Notifications.showWarning;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

public class MypyInspection extends LocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(MypyInspection.class);
    private static final List<Problem> NO_PROBLEMS_FOUND = Collections.emptyList();

    private MypyPlugin plugin(final Project project) {
        final MypyPlugin mypyPlugin = project.getComponent(MypyPlugin.class);
        if (mypyPlugin == null) {
            throw new IllegalStateException("Couldn't get mypy plugin");
        }
        return mypyPlugin;
    }

    @Override
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile psiFile,
                                         @NotNull final InspectionManager manager,
                                         final boolean isOnTheFly) {
        return asProblemDescriptors(asyncResultOf(() -> inspectFile(psiFile, manager), NO_PROBLEMS_FOUND),
                manager);
    }

    @Nullable
    public List<Problem> inspectFile(@NotNull final PsiFile psiFile,
                                     @NotNull final InspectionManager manager) {
        LOG.debug("Inspection has been invoked.");

        final MypyPlugin plugin = plugin(manager.getProject());

        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            scannableFiles.addAll(ScannableFile.createAndValidate(singletonList(psiFile), plugin));
            if (scannableFiles.isEmpty()) {
                return NO_PROBLEMS_FOUND;
            }
            ScanFiles scanFiles = new ScanFiles(plugin, Collections.singletonList(psiFile.getVirtualFile()));
            Map<PsiFile, List<Problem>> map = scanFiles.call();
            if (map.isEmpty()) {
                return NO_PROBLEMS_FOUND;
            }
            return map.get(psiFile);

        } catch (ProcessCanceledException | AssertionError e) {
            LOG.debug("Process cancelled when scanning: " + psiFile.getName());
            return NO_PROBLEMS_FOUND;

        } catch (MypyPluginParseException e) {
            LOG.debug("Parse exception caught when scanning: " + psiFile.getName(), e);
            return NO_PROBLEMS_FOUND;

        } catch (Throwable e) {
            handlePluginException(e, psiFile, manager.getProject());
            return NO_PROBLEMS_FOUND;

        } finally {
            scannableFiles.forEach(ScannableFile::deleteIfRequired);
        }
    }

    private void handlePluginException(final Throwable e,
                                       final @NotNull PsiFile psiFile,
                                       final @NotNull Project project) {
        if (e.getCause() != null && e.getCause() instanceof IOException) {
            showWarning(project, message("mypy.file-io-failed"));

        } else {
            LOG.warn("Mypy threw an exception when scanning: " + psiFile.getName(), e);
            showException(project, e);
        }
    }

    @NotNull
    private ProblemDescriptor[] asProblemDescriptors(final List<Problem> results, final InspectionManager manager) {
        return ofNullable(results)
                .map(problems -> problems.stream()
                        .map(problem -> problem.toProblemDescriptor(manager))
                        .toArray(ProblemDescriptor[]::new))
                .orElse(ProblemDescriptor.EMPTY_ARRAY);
    }
}
