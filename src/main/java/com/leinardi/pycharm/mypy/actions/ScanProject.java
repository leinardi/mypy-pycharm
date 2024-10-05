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
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ThrowableRunnable;
import com.leinardi.pycharm.mypy.MypyPlugin;
import com.leinardi.pycharm.mypy.toolwindow.MypyToolWindowPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Action to execute a Mypy scan on the current project.
 */
public class ScanProject extends BaseAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final MypyPlugin mypyPlugin = project.getService(MypyPlugin.class);
                if (mypyPlugin == null) {
                    throw new IllegalStateException("Couldn't get mypy plugin");
                }

                final ToolWindow toolWindow = ToolWindowManager.getInstance(
                        project).getToolWindow(MypyToolWindowPanel.ID_TOOLWINDOW);
                toolWindow.activate(() -> {
                    try {
                        setProgressText(toolWindow, "plugin.status.in-progress.project");
                        ThrowableRunnable<RuntimeException> scanAction;
                        //                                        if (scope == ScanScope.Everything) {
                        scanAction = new ScanEverythingAction(project);
                        //                    } else {
                        //                    final ProjectRootManager projectRootManager = ProjectRootManager
                        // .getInstance(project);
                        //                    final VirtualFile[] sourceRoots =
                        // projectRootManager.getContentSourceRoots();
                        //                    if (sourceRoots.length > 0) {
                        //                        scanAction = new ScanSourceRootsAction(project, sourceRoots/*,
                        //                                    getSelectedOverride(toolWindow)*/);
                        //                    }
                        //                    }
                        //                    if (scanAction != null) {
                        ReadAction.run(scanAction);
                        //                    }
                    } catch (Throwable e) {
                        MypyPlugin.processErrorAndLog("Project scan", e);
                    }
                });

            } catch (Throwable e) {
                MypyPlugin.processErrorAndLog("Project scan", e);
            }
        });
    }

    @Override
    public final void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();

        project(event).ifPresentOrElse(project -> {
            try {

                final MypyPlugin mypyPlugin = project.getService(MypyPlugin.class);
                if (mypyPlugin == null) {
                    throw new IllegalStateException("Couldn't get mypy plugin");
                }
                //            final ScanScope scope = mypyPlugin.configurationManager().getCurrent().getScanScope();

                VirtualFile[] sourceRoots;
                //            if (scope == ScanScope.Everything) {
                sourceRoots = new VirtualFile[]{ProjectUtil.guessProjectDir(project)};
                //            } else {
                //            final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
                //            sourceRoots = projectRootManager.getContentSourceRoots();
                //            }

                // disable if no files are selected or scan in progress
                if (containsAtLeastOneFile(sourceRoots)) {
                    presentation.setEnabled(!mypyPlugin.isScanInProgress());
                } else {
                    presentation.setEnabled(false);
                }
            } catch (Throwable e) {
                MypyPlugin.processErrorAndLog("Project button update", e);
            }
        }, () -> presentation.setEnabled(false));
    }

}
