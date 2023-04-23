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

import com.intellij.ui.JBColor;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

/**
 * The cell renderer for tree nodes in the tool window.
 */
public class ResultTreeRenderer extends JLabel
        implements TreeCellRenderer {

    private boolean selected;

    /**
     * Create a new cell renderer.
     */
    public ResultTreeRenderer() {
        super();
        setOpaque(false);
    }

    @Override
    public void paintComponent(final Graphics g) {
        g.setColor(getBackground());

        int offset = 0;
        if (getIcon() != null) {
            offset = getIcon().getIconWidth() + getIconTextGap();
        }

        g.fillRect(offset, 0, getWidth() - 1 - offset, getHeight() - 1);

        if (selected) {
            g.setColor(JBColor.getColor("Tree.selectionBorderColor"));
            g.drawRect(offset, 0, getWidth() - 1 - offset, getHeight() - 1);
        }

        super.paintComponent(g);
    }

    @Override
    public Component getTreeCellRendererComponent(final JTree tree,
                                                  final Object value,
                                                  final boolean selected,
                                                  final boolean expanded,
                                                  final boolean leaf,
                                                  final int row,
                                                  final boolean hasFocus) {
        this.selected = selected;

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (node != null) {
            final Object userObject = node.getUserObject();
            if (userObject instanceof ResultTreeNode) {
                final ResultTreeNode treeNode
                        = (ResultTreeNode) userObject;

                if (expanded) {
                    setIcon(treeNode.getExpandedIcon());
                } else {
                    setIcon(treeNode.getCollapsedIcon());
                }

                setToolTipText(treeNode.getTooltip());
                setText(treeNode.toString());
                validate();

            } else {
                setIcon(null);
            }
        }
        if (row == 0 && !leaf) {
            Font f = tree.getFont();
            setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        } else {
            setFont(tree.getFont());
        }

        setForeground(JBColor.getColor(selected
                ? "Tree.selectionForeground" : "Tree.textForeground"));
        setBackground(JBColor.getColor(selected
                ? "Tree.selectionBackground" : "Tree.textBackground"));

        return this;
    }
}
