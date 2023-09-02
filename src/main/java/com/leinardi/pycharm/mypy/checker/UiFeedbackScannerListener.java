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

import com.intellij.psi.PsiFile;
import com.leinardi.pycharm.mypy.MypyPlugin;
import com.leinardi.pycharm.mypy.exception.MypyPluginException;
import com.leinardi.pycharm.mypy.toolwindow.MypyToolWindowPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;

public class UiFeedbackScannerListener implements ScannerListener {
    private final MypyPlugin plugin;

    public UiFeedbackScannerListener(final MypyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void scanStarting(final List<PsiFile> filesToScan) {
        SwingUtilities.invokeLater(() -> {
            final MypyToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayInProgress(filesToScan.size());
            }
        });
    }

    @Override
    public void filesScanned(final int count) {
        SwingUtilities.invokeLater(() -> {
            final MypyToolWindowPanel toolWindowPanel = MypyToolWindowPanel.panelFor(plugin.getProject());
            if (toolWindowPanel != null) {
                toolWindowPanel.incrementProgressBarBy(count);
            }
        });
    }

    @Override
    public void scanCompletedSuccessfully(
            final Map<PsiFile, List<Problem>> scanResults) {
        SwingUtilities.invokeLater(() -> {
            final MypyToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayResults(scanResults);
            }
        });
    }

    @Override
    public void scanFailedWithError(final MypyPluginException error) {
        SwingUtilities.invokeLater(() -> {
            final MypyToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayErrorResult(error);
            }
        });
    }

    @Nullable
    private MypyToolWindowPanel toolWindowPanel() {
        return MypyToolWindowPanel.panelFor(plugin.getProject());
    }
}
