/**
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 * Copyright 2011-2013 Tirasa. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License"). You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at https://oss.oracle.com/licenses/CDDL
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at https://oss.oracle.com/licenses/CDDL.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.connid.bundles.csvdir;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Assert;
import org.junit.Test;

public class CSVDirConnectorUpdateTests extends AbstractTest {

    private static final String NEWMAIL = "newmail@newmail.com";

    @Test
    public final void updateTest()
            throws IOException {
        createFile("sample", TestAccountsValue.TEST_ACCOUNTS);

        final ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();

        // **test only**
        final APIConfiguration impl = TestHelpers.createTestConfiguration(
                CSVDirConnector.class, createConfiguration("sample.*\\.csv"));

        final ConnectorFacade facade = factory.newInstance(impl);

        Uid uid = new Uid("____jpc4323435;jPenelope");

        ConnectorObject object = facade.getObject(ObjectClass.ACCOUNT, uid, null);

        Assert.assertNotNull(object);
        Assert.assertEquals(object.getName().getNameValue(), uid.getUidValue());

        final CSVDirConnector connector = new CSVDirConnector();
        connector.init(createConfiguration("sample.*\\.csv"));

        Uid updatedAccount = connector.update(
                ObjectClass.ACCOUNT, uid, createSetOfAttributes(), null);

        Assert.assertEquals(uid.getUidValue(), updatedAccount.getUidValue());

        ConnectorObject objectUpdated = facade.getObject(ObjectClass.ACCOUNT, uid, null);

        Assert.assertNotNull(object);
        Assert.assertEquals(NEWMAIL,
                objectUpdated.getAttributeByName(TestAccountsValue.EMAIL).getValue().get(0));

        connector.dispose();
    }

    private Set<Attribute> createSetOfAttributes() {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(TestAccountsValue.EMAIL, NEWMAIL));
        return attributes;
    }

    @Test(expected = ConnectorException.class)
    public final void updateTestOfNotExistsUser()
            throws IOException {
        createFile("sample", TestAccountsValue.TEST_ACCOUNTS);
        Uid uid = new Uid("____jpc4323435;jPenelo");

        final CSVDirConnector connector = new CSVDirConnector();
        connector.init(createConfiguration("sample.*\\.csv"));
        Uid updatedAccount = connector.update(ObjectClass.ACCOUNT, uid,
                createSetOfAttributes(), null);
        Assert.assertEquals(uid.getUidValue(), updatedAccount.getUidValue());

        connector.dispose();
    }

    @Test
    public void issueCSVDIR12() throws IOException {
        final CSVDirConfiguration config = createConfiguration("issueCSVDIR12.*\\.csv");
        config.setMultivalueSeparator("|");
        config.validate();

        createFile("issueCSVDIR12", TestAccountsValue.TEST_ACCOUNTS);

        final ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        final APIConfiguration impl = TestHelpers.createTestConfiguration(CSVDirConnector.class, config);
        final ConnectorFacade connector = factory.newInstance(impl);

        Uid uid = new Uid("____jpc4323435;jPenelope");

        final ConnectorObject object = connector.getObject(ObjectClass.ACCOUNT, uid, null);

        Assert.assertNotNull(object);
        Assert.assertEquals(object.getName().getNameValue(), uid.getUidValue());

        final Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(TestAccountsValue.EMAIL, "mrossi1@tirasa.net", "mrossi2@tirasa.net"));

        final Uid updatedAccount = connector.update(ObjectClass.ACCOUNT, uid, attributes, null);
        Assert.assertEquals(uid.getUidValue(), updatedAccount.getUidValue());

        final OperationOptionsBuilder oob = new OperationOptionsBuilder();
        oob.setAttributesToGet(TestAccountsValue.EMAIL);

        final ConnectorObject obj = connector.getObject(ObjectClass.ACCOUNT, uid, oob.build());
        final List<Object> value = obj.getAttributeByName(TestAccountsValue.EMAIL).getValue();
        assertEquals(2, value.size(), 0);
        assertEquals("mrossi1@tirasa.net", value.get(0).toString());
        assertEquals("mrossi2@tirasa.net", value.get(1).toString());
    }
}