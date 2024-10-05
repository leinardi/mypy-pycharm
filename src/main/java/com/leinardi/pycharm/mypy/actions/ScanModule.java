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
 *
 * Modifications:
 * - 2024-10-03: Modified by Dominik Willner to fix ActionUpdateThread deprecation warnings.
 */

package com.leinardi.pycharm.mypy.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.ThrowableRunnable;
import com.leinardi.pycharm.mypy.MypyPlugin;
import com.leinardi.pycharm.mypy.util.VfUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.leinardi.pycharm.mypy.actions.ToolWindowAccess.toolWindow;

/**
 * Action to execute a Mypy scan on the current module.
 */
public class ScanModule extends BaseAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public final void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);

                final VirtualFile[] selectedFiles
                        = FileEditorManager.getInstance(project).getSelectedFiles();
                if (selectedFiles.length == 0) {
                    setProgressText(toolWindow, "plugin.status.in-progress.no-file");
                    return;
                }

                toolWindow.activate(() -> {
                    try {
                        setProgressText(toolWindow, "plugin.status.in-progress.module");

                        List<VirtualFile> moduleFiles = VfUtil.filterOnlyPythonProjectFiles(
                                project, VfUtil.flattenFiles(new VirtualFile[]{selectedFiles[0].getParent()}));
                        ThrowableRunnable<RuntimeException> scanAction = new ScanSourceRootsAction(project,
                                moduleFiles.toArray(new VirtualFile[0]));
                        ReadAction.run(scanAction);
                    } catch (Throwable e) {
                        MypyPlugin.processErrorAndLog("Current Module scan", e);
                    }
                });

            } catch (Throwable e) {
                MypyPlugin.processErrorAndLog("Current Module scan", e);
            }
        });
    }

    @Override
    public final void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();

        project(event).ifPresentOrElse(project -> {
            try {
                final VirtualFile[] selectedFiles
                        = FileEditorManager.getInstance(project).getSelectedFiles();
                if (selectedFiles.length == 0) {
                    return;
                }

                final Module module = ModuleUtil.findModuleForFile(
                        selectedFiles[0], project);
                if (module == null) {
                    return;
                }

                final MypyPlugin mypyPlugin
                        = project.getService(MypyPlugin.class);
                if (mypyPlugin == null) {
                    throw new IllegalStateException("Couldn't get mypy plugin");
                }

                VirtualFile[] moduleFiles;
                final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                moduleFiles = moduleRootManager.getContentRoots();

                // disable if no files are selected or scan in progress
                if (containsAtLeastOneFile(moduleFiles)) {
                    presentation.setEnabled(!mypyPlugin.isScanInProgress());
                } else {
                    presentation.setEnabled(false);
                }
            } catch (Throwable e) {
                MypyPlugin.processErrorAndLog("Current Module button update", e);
            }
        }, () -> presentation.setEnabled(false));
    }
}
