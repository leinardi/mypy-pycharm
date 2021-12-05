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

package com.leinardi.pycharm.mypy.handlers;

import com.intellij.CommonBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.psi.PsiFile;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.UIUtil;
import com.leinardi.pycharm.mypy.MypyConfigService;
import com.leinardi.pycharm.mypy.MypyPlugin;
import com.leinardi.pycharm.mypy.checker.Problem;
import com.leinardi.pycharm.mypy.toolwindow.MypyToolWindowPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.CANCEL;
import static com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.CLOSE_WINDOW;
import static com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult.COMMIT;
import static com.leinardi.pycharm.mypy.MypyBundle.message;
import static java.util.stream.Collectors.toList;

public class ScanFilesBeforeCheckinHandler extends CheckinHandler {
    private static final Logger LOG = Logger.getInstance(ScanFilesBeforeCheckinHandler.class);

    private final CheckinProjectPanel checkinPanel;
    private final MypyConfigService mypyConfigService;

    public ScanFilesBeforeCheckinHandler(@NotNull final CheckinProjectPanel myCheckinPanel) {
        this.checkinPanel = myCheckinPanel;
        mypyConfigService = MypyConfigService.getInstance(checkinPanel.getProject());
    }

    @Nullable
    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox checkBox = new JCheckBox(message("handler.before.checkin.checkbox"));

        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                final JPanel panel = new JPanel(new BorderLayout());
                panel.add(checkBox);
                return panel;
            }

            @Override
            public void refresh() {
            }

            @Override
            public void saveState() {
                mypyConfigService.setScanBeforeCheckin(checkBox.isSelected());
            }

            @Override
            public void restoreState() {
                checkBox.setSelected(mypyConfigService.isScanBeforeCheckin());
            }
        };
    }

    @Override
    public ReturnResult beforeCheckin(@Nullable final CommitExecutor executor,
                                      final PairConsumer<Object, Object> additionalDataConsumer) {
        final Project project = checkinPanel.getProject();
        if (project == null) {
            LOG.warn("Could not get project for check-in panel, skipping");
            return COMMIT;
        }

        final MypyPlugin plugin = project.getComponent(MypyPlugin.class);
        if (plugin == null) {
            LOG.warn("Could not get Mypy Plug-in, skipping");
            return COMMIT;
        }

        if (mypyConfigService.isScanBeforeCheckin()) {
            try {
                final Map<PsiFile, List<Problem>> scanResults = new HashMap<>();
                new Task.Modal(project, message("handler.before.checkin.scan.text"), false) {
                    @Override
                    public void run(@NotNull final ProgressIndicator progressIndicator) {
                        progressIndicator.setText(message("handler.before.checkin.scan.in-progress"));
                        progressIndicator.setIndeterminate(true);
                        scanResults.putAll(plugin.scanFiles(new ArrayList<>(checkinPanel.getVirtualFiles())));
                    }
                }.queue();

                return processScanResults(scanResults, executor, plugin);

            } catch (ProcessCanceledException e) {
                return CANCEL;
            }

        } else {
            return COMMIT;
        }
    }

    private ReturnResult processScanResults(final Map<PsiFile, List<Problem>> results,
                                            final CommitExecutor executor,
                                            final MypyPlugin plugin) {
        final int errorCount = errorCountOf(results);
        if (errorCount == 0) {
            return COMMIT;
        }

        final int answer = promptUser(plugin, errorCount, executor);
        if (answer == Messages.OK) {
            showResultsInToolWindow(results, plugin);
            return CLOSE_WINDOW;

        } else if (answer == Messages.CANCEL || answer < 0) {
            return CANCEL;
        }

        return COMMIT;
    }

    private int errorCountOf(final Map<PsiFile, List<Problem>> results) {
        return results.entrySet().stream()
                .filter(this::hasProblemsThatAreNotIgnored)
                .collect(toList())
                .size();
    }

    private boolean hasProblemsThatAreNotIgnored(final Map.Entry<PsiFile, List<Problem>> entry) {
        return entry.getValue().size() > 0;
    }

    private int promptUser(final MypyPlugin plugin,
                           final int errorCount,
                           final CommitExecutor executor) {
        String commitButtonText;
        if (executor != null) {
            commitButtonText = executor.getActionText();
        } else {
            commitButtonText = checkinPanel.getCommitActionName();
        }

        if (commitButtonText.endsWith("...")) {
            commitButtonText = commitButtonText.substring(0, commitButtonText.length() - 3);
        }

        final String[] buttons = new String[]{
                message("handler.before.checkin.error.review"),
                commitButtonText,
                CommonBundle.getCancelButtonText()};

        return Messages.showDialog(plugin.getProject(), message("handler.before.checkin.error.text", errorCount),
                message("handler.before.checkin.error.title"),
                buttons, 0, UIUtil.getWarningIcon());
    }

    private void showResultsInToolWindow(final Map<PsiFile, List<Problem>> results,
                                         final MypyPlugin plugin) {
        final MypyToolWindowPanel toolWindowPanel = MypyToolWindowPanel.panelFor(plugin.getProject());
        if (toolWindowPanel != null) {
            toolWindowPanel.displayResults(results);
            toolWindowPanel.showToolWindow();
        }
    }

}
