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
package org.connid.bundles.csvdir.methods;

import java.sql.SQLException;
import org.connid.bundles.csvdir.CSVDirConfiguration;
import org.connid.bundles.csvdir.CSVDirConnection;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Connector;

public class CSVDirDelete extends CommonOperation {

    /**
     * Setup {@link Connector} based logging.
     */
    private static final Log LOG = Log.getLog(CSVDirDelete.class);

    private final CSVDirConnection connection;

    private final CSVDirConfiguration configuration;

    private Uid uid = null;

    public CSVDirDelete(final CSVDirConfiguration configuration, final Uid uid)
            throws ClassNotFoundException, SQLException {

        this.configuration = configuration;
        this.uid = uid;
        this.connection = CSVDirConnection.openConnection(configuration);
    }

    public void execute() {
        try {
            executeImpl();
        } catch (Exception e) {
            LOG.error(e, "error during updating");
            throw new ConnectorException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.closeConnection();
                }
            } catch (SQLException e) {
                LOG.error(e, "Error closing connections");
            }
        }
    }

    private void executeImpl() throws SQLException {
        if (!userExists(uid.getUidValue(), connection, configuration)) {
            throw new ConnectorException("User does not exist");
        }
        connection.deleteAccount(uid);
        LOG.ok("Delete completed");
    }
}
