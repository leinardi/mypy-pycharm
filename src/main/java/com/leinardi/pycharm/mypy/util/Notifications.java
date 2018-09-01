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

import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import static com.intellij.notification.NotificationGroup.balloonGroup;
import static com.intellij.notification.NotificationGroup.logOnlyGroup;
import static com.intellij.notification.NotificationListener.URL_OPENING_LISTENER;
import static com.intellij.notification.NotificationType.ERROR;
import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.notification.NotificationType.WARNING;
import static com.leinardi.pycharm.mypy.MypyBundle.message;
import static com.leinardi.pycharm.mypy.util.Exceptions.rootCauseOf;

public final class Notifications {

    private static final NotificationGroup BALLOON_GROUP = balloonGroup(message("plugin.notification.alerts"));
    private static final NotificationGroup LOG_ONLY_GROUP = logOnlyGroup(message("plugin.notification.logging"));
    private static final String TITLE = message("plugin.name");

    private Notifications() {
    }

    public static void showInfo(final Project project, final String infoText) {
        BALLOON_GROUP
                .createNotification(TITLE, infoText, INFORMATION, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showInfo(final Project project, final String title, final String infoText) {
        BALLOON_GROUP
                .createNotification(title, infoText, INFORMATION, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showWarning(final Project project, final String warningText) {
        BALLOON_GROUP
                .createNotification(TITLE, warningText, WARNING, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showWarning(final Project project, final String title, final String warningText) {
        BALLOON_GROUP
                .createNotification(title, warningText, WARNING, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showError(final Project project, final String errorText) {
        BALLOON_GROUP
                .createNotification(TITLE, errorText, ERROR, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showError(final Project project, final String title, final String errorText) {
        BALLOON_GROUP
                .createNotification(title, errorText, ERROR, URL_OPENING_LISTENER)
                .notify(project);
    }

    public static void showException(final Project project, final Throwable t) {
        LOG_ONLY_GROUP
                .createNotification(message("plugin.exception"), messageFor(t), ERROR, URL_OPENING_LISTENER)
                .notify(project);
    }

    @NotNull
    private static String messageFor(final Throwable t) {
        if (t.getCause() != null) {
            return message("mypy.exception-with-root-cause", t.getMessage(), traceOf(rootCauseOf(t)));
        }
        return message("mypy.exception", traceOf(t));
    }

    private static String traceOf(final Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return t.getMessage() + "<br>" + sw.toString()
                .replaceAll("\t", "&nbsp;&nbsp;")
                .replaceAll("\n", "<br>");
    }

}
