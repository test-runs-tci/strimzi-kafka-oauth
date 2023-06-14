/*
 * Copyright 2017-2023, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.testsuite.oauth.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ContainerLogLineReader {

    private final String containerName;
    private int logLineOffset = 0;

    public ContainerLogLineReader(String containerName) {
        this.containerName = containerName;
    }

    public List<String> readNext() throws IOException {
        List<String> lines = new ArrayList<>();
        Process p = Runtime.getRuntime().exec(new String[] {"docker", "logs", containerName});
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
        }

        List<String> result = lines.subList(logLineOffset, lines.size());
        logLineOffset = lines.size();
        return result;
    }
}
