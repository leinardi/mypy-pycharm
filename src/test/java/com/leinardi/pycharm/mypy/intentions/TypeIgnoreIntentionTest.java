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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.python.PythonLanguage;

public class TypeIgnoreIntentionTest extends BasePlatformTestCase {
    private void assertFixerReplacement(String input, int caretPosition, String output) {
        PsiFile file = createLightFile("test.py", PythonLanguage.INSTANCE, input);
        PsiElement el = file.findElementAt(caretPosition);
        assertNotNull(el);

        TypeIgnoreIntention fixer = new TypeIgnoreIntention();
        assertTrue(fixer.isAvailable(file.getProject(), null, el));

        fixer.invoke(getProject(), null, el);
        assertEquals(output, file.getText());
    }

    @SuppressWarnings("SameParameterValue")
    private void assertNotAvailable(String input, int caretPosition) {
        PsiFile file = createLightFile("test.py", PythonLanguage.INSTANCE, input);
        PsiElement el = file.findElementAt(caretPosition);
        assertNotNull(el);

        TypeIgnoreIntention fixer = new TypeIgnoreIntention();
        assertFalse(fixer.isAvailable(file.getProject(), null, el));
    }

    public void testVariableAssignment() {
        String input = "import os\n" +
                "\n" +
                "foo: str = 123\n" +
                "print()\n";
        String output = "import os\n" +
                "\n" +
                "foo: str = 123  # type: ignore\n" +
                "print()\n";
        assertFixerReplacement(input, input.indexOf("123"), output);
    }

    public void testMultilineCall() {
        String input = "print(\n" +
                "    foo,\n" +
                "    bar,\n" +
                ")\n";
        String output = "print(\n" +
                "    foo,\n" +
                "    bar,  # type: ignore\n" +
                ")\n";
        assertFixerReplacement(input, input.indexOf("bar"), output);
    }

    public void testMultilineString() {
        // Trying to append comment to multiline string line would cause a syntax error.
        String input = "foo: int = \"\"\"\n" +
                "\"\"\"\n";
        assertNotAvailable(input, 0);
    }

    public void testPrependComment() {
        String input = "print()  # Hi I'm a comment\n";
        String output = "print()  # type: ignore # Hi I'm a comment\n";
        assertFixerReplacement(input, 0, output);
    }

    public void testCaretAtEndOfLine() {
        String input = "print()\n\n";
        String output = "print()  # type: ignore\n\n";
        assertFixerReplacement(input, input.indexOf("\n"), output);
    }

    public void testNotAvailable() {
        // `type: ignore` comment already present
        String input = "print()  # type: ignore    #Hi I'm a comment\n";
        assertNotAvailable(input, 0);
    }

    public void testAlmostEmptyFile() {
        String input = "\n";
        assertNotAvailable(input, 0);
    }

    public void testEmptyFile() {
        String input = "";
        // Can't use assertNotAvailable(): findElementAt() would fail.
        PsiFile file = createLightFile("test.py", PythonLanguage.INSTANCE, input);
        TypeIgnoreIntention fixer = new TypeIgnoreIntention();
        assertFalse(fixer.isAvailable(file.getProject(), null, file));
    }
}
