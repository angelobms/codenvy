/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.config;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestConfig {

    private ConfigFactory configFactory;

    @BeforeMethod
    public void setUp() throws Exception {
        configFactory = new ConfigFactory();
    }

    @Test
    public void testConfigProperties() throws Exception {
        Path conf = Paths.get("target", "conf.properties");

        FileUtils.write(conf.toFile(), "user=1\npwd=2\n");

        Map<String, String> m = configFactory.loadConfigProperties(conf);
        assertEquals(m.size(), 2);
        assertEquals(m.get("user"), "1");
        assertEquals(m.get("pwd"), "2");
    }
}
