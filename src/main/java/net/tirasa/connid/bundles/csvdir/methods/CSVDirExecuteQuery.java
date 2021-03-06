/**
 * Copyright (C) 2011 ConnId (connid-dev@googlegroups.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tirasa.connid.bundles.csvdir.methods;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.tirasa.connid.bundles.csvdir.CSVDirConfiguration;
import net.tirasa.connid.bundles.csvdir.CSVDirConnection;
import net.tirasa.connid.bundles.db.common.FilterWhereBuilder;
import net.tirasa.connid.bundles.db.common.SQLParam;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Connector;

public class CSVDirExecuteQuery extends CommonOperation {

    /**
     * Setup {@link Connector} based logging.
     */
    private static final Log LOG = Log.getLog(CSVDirExecuteQuery.class);

    private final CSVDirConfiguration conf;

    private final CSVDirConnection conn;

    private final ObjectClass oclass;

    private final FilterWhereBuilder where;

    private final ResultsHandler handler;

    private final OperationOptions options;

    public CSVDirExecuteQuery(final CSVDirConfiguration configuration,
            final ObjectClass oclass,
            final FilterWhereBuilder where,
            final ResultsHandler handler,
            final OperationOptions options)
            throws ClassNotFoundException, SQLException {

        this.conf = configuration;
        this.oclass = oclass;
        this.where = where;
        this.handler = handler;
        this.options = options;
        this.conn = CSVDirConnection.openConnection(configuration);
    }

    public void execute() {
        try {
            executeImpl();
        } catch (Exception e) {
            LOG.error(e, "error during updating");
            throw new ConnectorException(e);
        } finally {
            try {
                if (conn != null) {
                    conn.closeConnection();
                }
            } catch (SQLException e) {
                LOG.error(e, "Error closing connections");
            }
        }
    }

    private void executeImpl()
            throws SQLException {
        LOG.info("check the ObjectClass and result handler");

        // Contract tests
        if (oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Object class required");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Result handler required");
        }

        LOG.ok("The ObjectClass and result handler is ok");

        final Set<String> columnNamesToGet = resolveColumnNamesToGet();
        LOG.ok("Column Names {0} To Get", columnNamesToGet);

        final String whereClause = where == null ? null : where.getWhereClause();
        LOG.ok("Where Clause {0}", whereClause);

        final List<SQLParam> params = where == null ? null : where.getParams();
        LOG.ok("Where Params {0}", params);

        ResultSet resultSet = null;
        try {
            resultSet = conn.allCsvFiles(whereClause, params);

            boolean handled = true;

            while (resultSet.next() && handled) {
                if (StringUtil.isBlank(conf.getDeleteColumnName())
                        || !Boolean.valueOf(resultSet.getString(resultSet.findColumn(conf.getDeleteColumnName())))) {
                    // create the connector object..
                    handled = handler.handle(buildConnectorObject(conf, resultSet));
                }
            }
        } catch (final Exception e) {
            LOG.error(e, "Search query failed");
            throw new ConnectorIOException(e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                LOG.error(e, "Error closing result set");
            }
        }
        LOG.ok("Query Account commited");
    }

    private Set<String> resolveColumnNamesToGet() {
        final Set<String> attributesToGet = new HashSet<String>();
        attributesToGet.add(Uid.NAME);

        String[] attributes;
        if (options == null || options.getAttributesToGet() == null) {
            attributes = conf.getFields();
        } else {
            attributes = options.getAttributesToGet();
        }

        attributesToGet.addAll(Arrays.asList(attributes));
        return attributesToGet;
    }
}
