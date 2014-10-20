/* 
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 ConnId. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package net.tirasa.connid.bundles.csvdir;

import net.tirasa.connid.bundles.csvdir.CSVDirConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;
import org.junit.Test;

/**
 * Attempts to test that the configuration options can validate the input given them. It also attempt to make sure the
 * properties are correct.
 */
public class CSVDirConfigurationTests extends AbstractTest {

    /**
     * Tests setting and validating the parameters provided.
     */
    @Test
    public void testValidate() throws Exception {
        final CSVDirConfiguration config = new CSVDirConfiguration();
        // check defaults..
        assertNull(config.getFileMask());
        assertEquals(Charset.defaultCharset().name(), config.getEncoding());
        assertEquals('"', config.getTextQualifier());
        assertEquals(',', config.getFieldDelimiter());
        // set a unique attribute so there's not a runtime exception..
        config.setKeyColumnNames(new String[] {"uid"});

        // simple property test..
        config.setFileMask(".*\\.csv");
        assertEquals(".*\\.csv", config.getFileMask());

        // try the validate..
        try {
            config.validate();
            fail();
        } catch (RuntimeException e) {
            // expected because configuration is incomplete
        }

        config.setPasswordColumnName("password");
        config.setDeleteColumnName("deleted");
        config.setFields(new String[] {"accountid", "password", "deleted"});

        // create a temp file
        final File csv = File.createTempFile("sample", ".csv", testSourceDir);
        csv.deleteOnExit();

        config.setSourcePath(csv.getParent());

        // this should work..
        config.validate();

        // check encoding..
        config.setEncoding(Charset.forName("UTF-8").name());
        assertEquals(Charset.forName("UTF-8").name(), config.getEncoding());

        // test stupid problem..
        config.setFieldDelimiter('"');
        try {
            config.validate();
            fail();
        } catch (IllegalStateException ex) {
            // should go here..
        }
        // fix field delimiter..
        config.setFieldDelimiter(',');

        // test blank unique attribute..
        try {
            config.setKeyColumnNames(new String[] {});
            config.validate();
            fail();
        } catch (IllegalArgumentException ex) {
            // should throw..
        }
    }
}
