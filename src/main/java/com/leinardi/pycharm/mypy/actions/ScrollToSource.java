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
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.leinardi.pycharm.mypy.MypyPlugin;
import com.leinardi.pycharm.mypy.toolwindow.MypyToolWindowPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Toggle the scroll to source setting.
 */
public final class ScrollToSource extends DumbAwareToggleAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public boolean isSelected(final AnActionEvent event) {
        final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return false;
        }

        final MypyPlugin mypyPlugin
                = project.getService(MypyPlugin.class);
        if (mypyPlugin == null) {
            throw new IllegalStateException("Couldn't get mypy plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(MypyToolWindowPanel.ID_TOOLWINDOW);

        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null && content.getComponent() instanceof MypyToolWindowPanel) {
            return ((MypyToolWindowPanel) content.getComponent()).isScrollToSource();
        }

        return true;
    }

    @Override
    public void setSelected(final AnActionEvent event, final boolean selected) {
        final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return;
        }

        final MypyPlugin mypyPlugin
                = project.getService(MypyPlugin.class);
        if (mypyPlugin == null) {
            throw new IllegalStateException("Couldn't get mypy plugin");
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(
                project).getToolWindow(MypyToolWindowPanel.ID_TOOLWINDOW);

        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null && content.getComponent() instanceof MypyToolWindowPanel) {
            ((MypyToolWindowPanel) content.getComponent()).setScrollToSource(selected);
        }
    }
}
