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

package com.leinardi.pycharm.mypy.toolwindow;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.leinardi.pycharm.mypy.MypyBundle;
import com.leinardi.pycharm.mypy.checker.Problem;
import com.leinardi.pycharm.mypy.mpapi.SeverityLevel;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Comparator.comparing;

public class ResultTreeModel extends DefaultTreeModel {

    private static final long serialVersionUID = 2161855162879365203L;

    private final DefaultMutableTreeNode visibleRootNode;

    public ResultTreeModel() {
        super(new DefaultMutableTreeNode());

        visibleRootNode = new DefaultMutableTreeNode();
        ((DefaultMutableTreeNode) getRoot()).add(visibleRootNode);

        setRootMessage(null);
    }

    public void clear() {
        visibleRootNode.removeAllChildren();
        nodeStructureChanged(visibleRootNode);
    }

    public TreeNode getVisibleRoot() {
        return visibleRootNode;
    }

    /**
     * Set the root message.
     * <p>
     * This will trigger a reload on the model, thanks to JTree's lack of support for
     * a node changed event for the root node.
     *
     * @param messageText the text to display.
     */
    public void setRootText(@Nullable final String messageText) {
        if (messageText == null) {
            visibleRootNode.setUserObject(new ResultTreeNode(MypyBundle.message("plugin.results.no-scan")));

        } else {
            visibleRootNode.setUserObject(new ResultTreeNode(messageText));
        }

        nodeChanged(visibleRootNode);
    }

    /**
     * Set the root message.
     * <p>
     * This will trigger a reload on the model, thanks to JTree's lack of support for
     * a node changed event for the root node.
     *
     * @param messageKey the message key to display.
     */
    public void setRootMessage(@Nullable final String messageKey,
                               @Nullable final Object... messageArgs) {
        if (messageKey == null) {
            setRootText(null);

        } else {
            setRootText(MypyBundle.message(messageKey, messageArgs));
        }
    }

    /**
     * Display only the passed severity levels.
     *
     * @param levels the levels. Null is treated as 'none'.
     */
    public void filter(final SeverityLevel... levels) {
        filter(true, levels);
    }

    private void filter(final boolean sendEvents, final SeverityLevel... levels) {
        final Set<TogglableTreeNode> changedNodes = new HashSet<>();

        for (int fileIndex = 0; fileIndex < visibleRootNode.getChildCount(); ++fileIndex) {
            final TogglableTreeNode fileNode = (TogglableTreeNode) visibleRootNode.getChildAt(fileIndex);

            for (final TogglableTreeNode problemNode : fileNode.getAllChildren()) {
                final ResultTreeNode result = (ResultTreeNode) problemNode.getUserObject();

                final boolean currentVisible = problemNode.isVisible();
                final boolean desiredVisible = contains(levels, result.getSeverity());
                if (currentVisible != desiredVisible) {
                    problemNode.setVisible(desiredVisible);

                    changedNodes.add(fileNode);
                }
            }
        }

        if (sendEvents) {
            for (final TogglableTreeNode node : changedNodes) {
                nodeStructureChanged(node);
            }
        }
    }

    /*
     * This is a port from commons-lang 2.4, in order to get around the absence of commons-lang in
     * some packages of IDEA 7.x.
     */
    private boolean contains(final Object[] array, final Object objectToFind) {
        if (array == null) {
            return false;
        }
        if (objectToFind == null) {
            for (final Object anArray : array) {
                if (anArray == null) {
                    return true;
                }
            }
        } else {
            for (final Object anArray : array) {
                if (objectToFind.equals(anArray)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Set the displayed model.
     *
     * @param results the model.
     * @param levels  the levels to display.
     */
    public void setModel(final Map<PsiFile, List<Problem>> results,
                         final SeverityLevel... levels) {
        visibleRootNode.removeAllChildren();

        boolean hasProblems = false;
        int[] totalCounts = new int[SeverityLevel.values().length];
        for (final PsiFile file : sortedFileNames(results)) {
            final TogglableTreeNode fileNode = new TogglableTreeNode();
            final List<Problem> problems = results.get(file);

            int[] fileCounts = new int[SeverityLevel.values().length];
            if (problems != null) {
                for (final Problem problem : problems) {
                    final ResultTreeNode problemObj = new ResultTreeNode(file, problem);

                    final TogglableTreeNode problemNode = new TogglableTreeNode(problemObj);
                    fileNode.add(problemNode);
                    fileCounts[problem.severityLevel().ordinal()]++;
                }

                for (int i = 0; i < totalCounts.length; i++) {
                    totalCounts[i] += fileCounts[i];
                }

                if (!problems.isEmpty()) {
                    final ResultTreeNode nodeObject = new ResultTreeNode(file.getName(), fileCounts);
                    fileNode.setUserObject(nodeObject);
                    visibleRootNode.add(fileNode);
                    hasProblems = true;
                }
            }
        }

        if (hasProblems) {
            setRootText(StringUtil.pluralize(
                    MypyBundle.message("plugin.results.scan-results",
                            concatProblems(totalCounts),
                            results.size()), results.size()));
        } else {
            setRootMessage("plugin.results.scan-no-results");
        }

        filter(false, levels);
        nodeStructureChanged(visibleRootNode);
    }

    private Iterable<PsiFile> sortedFileNames(final Map<PsiFile, List<Problem>> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        final List<PsiFile> sortedFiles = new ArrayList<>(results.keySet());
        sortedFiles.sort(comparing(PsiFileSystemItem::getName));
        return sortedFiles;
    }

    static String concatProblems(int[] problemCounts) {
        StringBuilder violations = new StringBuilder();
        int errorsCount = problemCounts[SeverityLevel.ERROR.ordinal()];
        if (errorsCount > 0) {
            violations.append(StringUtil.pluralize(
                    MypyBundle.message("plugin.results.scan-results.error", errorsCount),
                    errorsCount));
            violations.append(", ");
        }
        int warningCount = problemCounts[SeverityLevel.WARNING.ordinal()];
        if (warningCount > 0) {
            violations.append(StringUtil.pluralize(
                    MypyBundle.message("plugin.results.scan-results.warning", warningCount),
                    warningCount));
            violations.append(", ");
        }
        int conventionCount = problemCounts[SeverityLevel.NOTE.ordinal()];
        if (conventionCount > 0) {
            violations.append(StringUtil.pluralize(
                    MypyBundle.message("plugin.results.scan-results.note", conventionCount),
                    conventionCount));
            violations.append(", ");
        }
        return new String(violations.deleteCharAt(violations.length() - 2));
    }
}
