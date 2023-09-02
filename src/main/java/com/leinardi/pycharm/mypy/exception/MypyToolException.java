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

package com.leinardi.pycharm.mypy.exception;

/**
 * Wrapper for an exception that occurred in the Mypy tool itself.
 * <p><b>Important:</b> Be sure to throw it <em>only</em> from the 'csaccess' sourceset!</p>
 */
public class MypyToolException extends MypyServiceException {

    public MypyToolException(String message) {
        super(message);
    }

    public MypyToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
