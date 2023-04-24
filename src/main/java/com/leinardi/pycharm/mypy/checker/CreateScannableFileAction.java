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

package com.leinardi.pycharm.mypy.checker;

import com.intellij.psi.PsiFile;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Action to read the file to a temporary file.
 */
class CreateScannableFileAction implements ThrowableRunnable<RuntimeException> {

    /**
     * Any failure that occurred on the thread.
     */
    private IOException failure;

    private final PsiFile psiFile;

    /**
     * The created temporary file.
     */
    private ScannableFile file;

    /**
     * Create a thread to read the given file to a temporary file.
     *
     * @param psiFile the file to read.
     */
    CreateScannableFileAction(@NotNull final PsiFile psiFile) {
        this.psiFile = psiFile;
    }

    /**
     * Get any failure that occurred in this thread.
     *
     * @return the failure, if any.
     */
    public IOException getFailure() {
        return failure;
    }

    /**
     * Get the scannable file.
     *
     * @return the scannable file.
     */
    public ScannableFile getFile() {
        return file;
    }

    @Override
    public void run() {
        try {
            file = new ScannableFile(psiFile);

        } catch (IOException e) {
            failure = e;
        }
    }
}
