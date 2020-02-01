/*
 * Copyright 2018 Roberto Leinardi.
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

package com.leinardi.pycharm.mypy.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public final class Async {
    private static final int FIFTY_MS = 50;

    private Async() {
    }

    public static <T> Future<T> executeOnPooledThread(final Callable<T> callable) {
        return ApplicationManager.getApplication().executeOnPooledThread(callable);
    }

    public static <T> Future<T> whenFinished(final Future<T> future) {
        while (!future.isDone() && !future.isCancelled()) {
            ProgressManager.checkCanceled();
            waitFor(FIFTY_MS);
        }
        return future;
    }

    private static void waitFor(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
