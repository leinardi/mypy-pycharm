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

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.leinardi.pycharm.mypy.checker.Problem;
import com.leinardi.pycharm.mypy.checker.ScanFiles;
import com.leinardi.pycharm.mypy.checker.ScannerListener;
import com.leinardi.pycharm.mypy.checker.UiFeedbackScannerListener;
import com.leinardi.pycharm.mypy.exception.MypyPluginException;
import com.leinardi.pycharm.mypy.util.Async;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static com.leinardi.pycharm.mypy.util.Async.whenFinished;

/**
 * Main class for the Mypy scanning plug-in.
 */
public final class MypyPlugin implements ProjectComponent {

    /**
     * The plugin ID. Caution: It must be identical to the String set in build.gradle at intellij.pluginName
     */
    public static final String ID_PLUGIN = "Mypy-PyCharm";

    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(MypyPlugin.class);

    private final Set<Future<?>> checksInProgress = new HashSet<>();
    private final Project project;

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public MypyPlugin(@NotNull final Project project) {
        this.project = project;

        LOG.info("Mypy Plugin loaded with project base dir: \"" + getProjectPath() + "\"");

    }

    public Project getProject() {
        return project;
    }

    @Nullable
    private File getProjectPath() {
        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }

    /**
     * Is a scan in progress?
     * <p>
     * This is only expected to be called from the event thread.
     *
     * @return true if a scan is in progress.
     */
    public boolean isScanInProgress() {
        synchronized (checksInProgress) {
            return !checksInProgress.isEmpty();
        }
    }

    @Override
    public void projectOpened() {
        LOG.debug("Project opened.");
    }

    @Override
    public void projectClosed() {
        LOG.debug("Project closed.");
    }

    @Override
    @NotNull
    public String getComponentName() {
        return ID_PLUGIN;
    }

    public static void processErrorAndLog(@NotNull final String action, @NotNull final Throwable e) {
        LOG.warn(action + " failed", e);
    }

    private <T> Future<T> checkInProgress(final Future<T> checkFuture) {
        synchronized (checksInProgress) {
            if (!checkFuture.isDone()) {
                checksInProgress.add(checkFuture);
            }
        }
        return checkFuture;
    }

    public void stopChecks() {
        synchronized (checksInProgress) {
            checksInProgress.forEach(task -> task.cancel(true));
            checksInProgress.clear();
        }
    }

    public <T> void checkComplete(final Future<T> task) {
        if (task == null) {
            return;
        }

        synchronized (checksInProgress) {
            checksInProgress.remove(task);
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void asyncScanFiles(final List<VirtualFile> files) {
        LOG.info("Scanning current file(s).");

        if (files == null || files.isEmpty()) {
            LOG.debug("No files provided.");
            return;
        }

        final ScanFiles checkFiles = new ScanFiles(this, files);
        checkFiles.addListener(new UiFeedbackScannerListener(this));
        runAsyncCheck(checkFiles);
    }

    public Map<PsiFile, List<Problem>> scanFiles(@NotNull final List<VirtualFile> files) {
        if (files.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return whenFinished(runAsyncCheck(new ScanFiles(this, files))).get();
        } catch (final Throwable e) {
            LOG.warn("ERROR scanning files", e);
            return Collections.emptyMap();
        }
    }

    private Future<Map<PsiFile, List<Problem>>> runAsyncCheck(final ScanFiles checker) {
        final Future<Map<PsiFile, List<Problem>>> checkFilesFuture =
                checkInProgress(Async.executeOnPooledThread(checker));
        checker.addListener(new ScanCompletionTracker(checkFilesFuture));
        return checkFilesFuture;
    }

    private class ScanCompletionTracker implements ScannerListener {

        private final Future<Map<PsiFile, List<Problem>>> future;

        ScanCompletionTracker(final Future<Map<PsiFile, List<Problem>>> future) {
            this.future = future;
        }

        @Override
        public void scanStarting(final List<PsiFile> filesToScan) {
        }

        @Override
        public void filesScanned(final int count) {
        }

        @Override
        public void scanCompletedSuccessfully(
                final Map<PsiFile, List<Problem>> scanResults) {
            checkComplete(future);
        }

        @Override
        public void scanFailedWithError(final MypyPluginException error) {
            checkComplete(future);
        }
    }
}
