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

package com.leinardi.pycharm.mypy.mpapi;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MypyRunnerTest {
    private static InputStream stringToStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testParseWithColon() throws IOException {
        String message = "Dict entry 0 has incompatible type \"int\": \"str\"; expected \"int\": \"int\"  [dict-item]";
        String input = "path/testfile.py:1:22: error: " + message + "\n";
        Issue parsed = new Issue("path/testfile.py", 1, 21, SeverityLevel.ERROR, message);

        List<Issue> results = MypyRunner.parseMypyOutput(stringToStream(input));
        Assert.assertArrayEquals(results.toArray(), new Issue[]{parsed});
    }
}
