/*
 * Snow-Globe
 *
 * Copyright 2017 The Kroger Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kroger.oss.snowGlobe;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

class PortMapper {

    private Map<Integer, Integer> nginxToDockerPortMap = new HashMap<>();
    private Map<String, Integer> urlRegexToNginxPortMap = new HashMap<>();

    Integer getMappedPortForUrl(String url) {
        return nginxToDockerPortMap.get(getNginxPort(url));
    }

    List<String> getComposePortMapping() {
        return nginxToDockerPortMap.entrySet().stream()
                .map(entry -> entry.getValue() + ":" + entry.getKey())
                .collect(toList());
    }

    void initMapping(TestFrameworkProperties testFrameworkProperties) {
        List<Map<String, Object>> yamlMapping = testFrameworkProperties.getNginxPortMapping();
        urlRegexToNginxPortMap = yamlMapping.stream()
                .collect(toMap(mapping -> valueOf(getActualMappingForPort(mapping).get("pattern")),
                        mapping -> parseInt(valueOf(getActualMappingForPort(mapping).get("port")))));
        nginxToDockerPortMap = urlRegexToNginxPortMap.entrySet().stream()
                .collect(toMap(Map.Entry::getValue, entry -> getAvailablePort()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getActualMappingForPort(Map<String, Object> singlePortMapping) {
        return (Map<String, Object>) singlePortMapping.entrySet().stream()
                .findFirst().orElseThrow(() -> new RuntimeException("Invalid port mapping entry"))
                .getValue();
    }

    private Integer getNginxPort(String url) {
        return urlRegexToNginxPortMap.entrySet().stream()
                .filter(entry -> url.matches(entry.getKey()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to map url request: \"" + url + "\" to known port in the yaml configuration."))
                .getValue();
    }

    public static int getAvailablePort() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int localPort = serverSocket.getLocalPort();
            serverSocket.close();
            return localPort;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
