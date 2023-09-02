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

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.leinardi.pycharm.mypy.MypyPlugin;
import com.leinardi.pycharm.mypy.exception.MypyPluginException;
import com.leinardi.pycharm.mypy.mpapi.Issue;
import com.leinardi.pycharm.mypy.mpapi.MypyRunner;
import com.leinardi.pycharm.mypy.mpapi.ProcessResultsThread;
import com.leinardi.pycharm.mypy.util.Notifications;
import org.jetbrains.annotations.NotNull;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static java.util.Collections.emptyMap;

public class ScanFiles implements Callable<Map<PsiFile, List<Problem>>> {

    private static final Logger LOG = Logger.getInstance(ScanFiles.class);

    private final List<PsiFile> files;
    private final Set<ScannerListener> listeners = new HashSet<>();
    private final MypyPlugin plugin;

    public ScanFiles(@NotNull final MypyPlugin mypyPlugin,
                     @NotNull final List<VirtualFile> virtualFiles) {
        this.plugin = mypyPlugin;

        files = findAllFilesFor(virtualFiles);
    }

    private List<PsiFile> findAllFilesFor(@NotNull final List<VirtualFile> virtualFiles) {
        final List<PsiFile> childFiles = new ArrayList<>();
        final PsiManager psiManager = PsiManager.getInstance(this.plugin.getProject());
        for (final VirtualFile virtualFile : virtualFiles) {
            childFiles.addAll(buildFilesList(psiManager, virtualFile));
        }
        return childFiles;
    }

    @Override
    public final Map<PsiFile, List<Problem>> call() {
        try {
            fireCheckStarting(files);
            return scanCompletedSuccessfully(checkFiles(new HashSet<>(files)));
        } catch (final InterruptedIOException | InterruptedException e) {
            LOG.debug("Scan cancelled by PyCharm", e);
            return scanCompletedSuccessfully(emptyMap());
        } catch (final MypyPluginException e) {
            LOG.warn("An error occurred while scanning a file.", e);
            return scanFailedWithError(e);
        } catch (final Throwable e) {
            LOG.warn("An error occurred while scanning a file.", e);
            return scanFailedWithError(new MypyPluginException("An error occurred while scanning a file.", e));
        }
    }

    private Map<String, PsiFile> mapFilesToElements(final List<ScannableFile> filesToScan) {
        final Map<String, PsiFile> filePathsToElements = new HashMap<>();
        for (ScannableFile scannableFile : filesToScan) {
            filePathsToElements.put(scannableFile.getAbsolutePath(), scannableFile.getPsiFile());
        }
        return filePathsToElements;
    }

    private Map<PsiFile, List<Problem>> checkFiles(final Set<PsiFile> filesToScan)
            throws InterruptedIOException, InterruptedException {
        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            scannableFiles.addAll(ScannableFile.createAndValidate(filesToScan, plugin));
            return scan(scannableFiles);
        } finally {
            scannableFiles.forEach(ScannableFile::deleteIfRequired);
        }
    }

    private Map<PsiFile, List<Problem>> scan(final List<ScannableFile> filesToScan)
            throws InterruptedIOException, InterruptedException {
        Map<String, PsiFile> fileNamesToPsiFiles = mapFilesToElements(filesToScan);
        List<Issue> errors = MypyRunner.scan(plugin.getProject(), fileNamesToPsiFiles.keySet());
        String baseDir = plugin.getProject().getBasePath();
        int tabWidth = 4;
        final ProcessResultsThread findThread = new ProcessResultsThread(false, tabWidth, baseDir,
                errors, fileNamesToPsiFiles);

        ReadAction.run(findThread);
        return findThread.getProblems();
    }

    private Map<PsiFile, List<Problem>> scanFailedWithError(final MypyPluginException e) {
        Notifications.showException(plugin.getProject(), e);
        fireScanFailedWithError(e);

        return emptyMap();
    }

    private Map<PsiFile, List<Problem>> scanCompletedSuccessfully(final Map<PsiFile, List<Problem>> filesToProblems) {
        fireScanCompletedSuccessfully(filesToProblems);
        return filesToProblems;
    }

    public void addListener(final ScannerListener listener) {
        listeners.add(listener);
    }

    private void fireCheckStarting(final List<PsiFile> filesToScan) {
        listeners.forEach(listener -> listener.scanStarting(filesToScan));
    }

    private void fireScanCompletedSuccessfully(
            final Map<PsiFile, List<Problem>> fileResults) {
        listeners.forEach(listener -> listener.scanCompletedSuccessfully(fileResults));
    }

    private void fireScanFailedWithError(final MypyPluginException error) {
        listeners.forEach(listener -> listener.scanFailedWithError(error));
    }

    private List<PsiFile> buildFilesList(final PsiManager psiManager, final VirtualFile virtualFile) {
        final List<PsiFile> allChildFiles = new ArrayList<>();
        ReadAction.run(() -> {
            final FindChildFiles visitor = new FindChildFiles(virtualFile, psiManager);
            VfsUtilCore.visitChildrenRecursively(virtualFile, visitor);
            allChildFiles.addAll(visitor.locatedFiles);
        });
        return allChildFiles;
    }

    private static class FindChildFiles extends VirtualFileVisitor {

        private final VirtualFile virtualFile;
        private final PsiManager psiManager;

        final List<PsiFile> locatedFiles = new ArrayList<>();

        FindChildFiles(final VirtualFile virtualFile, final PsiManager psiManager) {
            this.virtualFile = virtualFile;
            this.psiManager = psiManager;
        }

        @Override
        public boolean visitFile(@NotNull final VirtualFile file) {
            if (!file.isDirectory()) {
                final PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    locatedFiles.add(psiFile);
                }
            }
            return true;
        }
    }

}
