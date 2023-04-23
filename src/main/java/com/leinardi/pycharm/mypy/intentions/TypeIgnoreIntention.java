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

package com.leinardi.pycharm.mypy.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.psi.PyElementGenerator;
import com.leinardi.pycharm.mypy.MypyBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Intention action to append `# type: ignore` comment to suppress Mypy annotations.
 */
public class TypeIgnoreIntention extends PsiElementBaseIntentionAction implements IntentionAction {
    @NotNull
    @Override
    public String getText() {
        return MypyBundle.message("intention.type-ignore.text");
    }

    /**
     * This string is also used for the directory name containing the intention description.
     */
    @NotNull
    @Override
    public String getFamilyName() {
        return "TypeIgnoreIntention";
    }

    @NotNull
    String getCommentText() {
        return "# type: ignore";
    }

    /**
     * Checks whether this intention is available at the caret offset in file.
     */
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @Nullable PsiElement element) {
        if (element == null) {
            return false;
        }

        PsiElement lastNode = findElementBeforeNewline(element);
        if (lastNode == null) {
            return false;
        }

        if (!isComment(lastNode)) {
            // No comment - we can add one. Make sure it has a parent.
            return lastNode.getParent() != null;
        } else {
            PsiComment oldComment = (PsiComment) lastNode;
            // Extract the first part of comment, e.g.
            // "# type: ignore # Bla bla" -> "# type: ignore"
            String firstCommentPart = oldComment.getText().split("(?<!^)#", 2)[0].trim();
            return !firstCommentPart.equals(getCommentText());
        }
    }

    /**
     * Modifies the Psi to append the `# type: ignore` comment.
     *
     * @throws IncorrectOperationException Thrown by underlying (Psi model) write action context
     *                                     when manipulation of the psi tree fails.
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
            throws IncorrectOperationException {

        PsiElement lastNode = findElementBeforeNewline(element);
        assert lastNode != null : "Unexpected null node";

        if (isComment(lastNode)) {
            PsiComment oldComment = (PsiComment) lastNode;
            // Prepend to existing comment
            String text = getCommentText() + " " + oldComment.getText();
            oldComment.replace(createComment(element, text));
        } else {
            // Create a new comment at end of line
            PsiComment newComment = createComment(lastNode, getCommentText());

            assert lastNode.getParent() != null : "Unexpected null parent for " + lastNode;

            // Inserting elements in the other order causes the comment to appear on the wrong line (?!)
            PsiElement addedElement = lastNode.getParent().addAfter(newComment, lastNode);
            addedElement.getParent().addBefore(createSpace(addedElement), addedElement);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    boolean isComment(@NotNull PsiElement element) {
        return element instanceof PsiComment
                && ((PsiComment) element).getTokenType() == PyTokenTypes.END_OF_LINE_COMMENT;
    }

    /**
     * Inspired by PyPsiUtils.findSameLineComment() - but that function does not behave correctly when caret is placed
     * at the end of a line.
     */
    @Nullable
    PsiElement findElementBeforeNewline(@NotNull PsiElement element) {
        PsiElement elem = PsiTreeUtil.prevLeaf(element);
        PsiElement next = PsiTreeUtil.getDeepestFirst(element);
        while (true) {
            if (next == null) {
                // End of file
                return elem;
            }
            if (next.textContains('\n')) {
                if (next instanceof PsiWhiteSpace) {
                    return elem;
                } else {
                    // If newline occurs not in whitespace (e.g. multiline string), just disable inspection for now.
                    return null;
                }
            }
            elem = next;
            next = PsiTreeUtil.nextLeaf(next);
        }
    }

    PsiComment createComment(@NotNull PsiElement baseElement, @NotNull String text) {
        PyElementGenerator generator = PyElementGenerator.getInstance(baseElement.getProject());
        return generator.createFromText(LanguageLevel.forElement(baseElement), PsiComment.class, text);
    }

    /**
     * Generate two spaces before the comment.
     * Per PEP 8, inline comments should be sparated by at least two spaces:
     * https://www.python.org/dev/peps/pep-0008/#inline-comments
     */
    PsiWhiteSpace createSpace(@NotNull PsiElement baseElement) {
        PyElementGenerator generator = PyElementGenerator.getInstance(baseElement.getProject());
        return generator.createFromText(LanguageLevel.forElement(baseElement), PsiWhiteSpace.class, "  ");
    }
}
