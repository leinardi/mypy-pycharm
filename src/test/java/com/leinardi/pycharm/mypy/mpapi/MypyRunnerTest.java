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
        Issue parsed = new Issue("path/testfile.py", 1, 22, SeverityLevel.ERROR, message);

        List<Issue> results = MypyRunner.parseMypyOutput(stringToStream(input));
        Assert.assertArrayEquals(results.toArray(), new Issue[]{parsed});
    }
}
