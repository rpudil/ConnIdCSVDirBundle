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
package net.tirasa.connid.bundles.csvdir;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.tirasa.connid.bundles.csvdir.database.FileSystem;
import net.tirasa.connid.bundles.csvdir.database.FileToDB;
import net.tirasa.connid.bundles.csvdir.database.QueryCreator;
import net.tirasa.connid.bundles.csvdir.utilities.Utilities;
import net.tirasa.connid.bundles.db.common.DatabaseConnection;
import net.tirasa.connid.bundles.db.common.SQLParam;
import net.tirasa.connid.bundles.db.common.SQLUtil;
import org.hsqldb.jdbcDriver;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Uid;

public class CSVDirConnection {

    /**
     * Setup logging for the {@link DatabaseConnection}.
     */
    private static final Log LOG = Log.getLog(CSVDirConnection.class);

    private static final String HSQLDB_JDBC_URL_PREFIX = "jdbc:hsqldb:file:";

    private static final String HSQLDB_DB_NAME = "csvdir_db";

    private final String viewname;

    private final String query;

    private final String jdbcUrl;

    private final Set<String> tables = new HashSet<String>();

    private final Connection conn;

    private final CSVDirConfiguration conf;

    private final FileSystem fileSystem;

    private final FileToDB fileToDB;

    private CSVDirConnection(final CSVDirConfiguration conf)
            throws ClassNotFoundException, SQLException {

        this.conf = conf;
        this.fileSystem = new FileSystem(conf);

        Class.forName(jdbcDriver.class.getName());
        this.jdbcUrl = HSQLDB_JDBC_URL_PREFIX + conf.getSourcePath() + File.separator
                + HSQLDB_DB_NAME + ";shutdown=false";
        this.conn = DriverManager.getConnection(jdbcUrl, "sa", "");
        this.conn.setAutoCommit(true);

        this.viewname = "USER_EX" + Utilities.randomNumber();
        this.query = "SELECT * FROM " + viewname;

        this.fileToDB = new FileToDB(this);
    }

    public static CSVDirConnection openConnection(
            final CSVDirConfiguration configuration)
            throws ClassNotFoundException, SQLException {

        return new CSVDirConnection(configuration);
    }

    public void closeConnection()
            throws SQLException {

        if (this.conn != null) {
            LOG.ok("Closing connection ...");

            dropTableAndViewIfExists();
            this.conn.close();

            tables.clear();
        }
    }

    public int insertAccount(final Map<String, String> attributes) {
        final String tableName = fileToDB.createDbForCreate();

        tables.add(tableName);

        return execute(QueryCreator.insertQuery(attributes, tableName));
    }

    public int updateAccount(final Map<String, String> attrToBeReplaced, final Uid uid) {
        final File[] files = fileSystem.getAllCsvFiles();
        if (files.length == 0) {
            throw new ConnectorException("Empty table");
        }

        int returnValue = 0;

        for (File file : files) {
            final String tableName = fileToDB.createDbForUpdate(file);

            tables.add(tableName);

            returnValue += execute(QueryCreator.updateQuery(
                    attrToBeReplaced,
                    uid,
                    conf.getKeyseparator(),
                    conf.getKeyColumnNames(),
                    tableName));
        }
        return returnValue;
    }

    public int deleteAccount(final Uid uid) {
        final File[] files = fileSystem.getAllCsvFiles();
        if (files.length == 0) {
            throw new ConnectorException("Empty table");
        }

        int returnValue = 0;

        for (File file : files) {
            final String tableName = fileToDB.createDbForUpdate(file);

            tables.add(tableName);

            returnValue += execute(QueryCreator.deleteQuery(
                    uid,
                    conf.getKeyseparator(),
                    conf.getKeyColumnNames(),
                    tableName));

        }
        return returnValue;
    }

    private int execute(final String query) {
        PreparedStatement stm = null;

        LOG.ok("About to execute {0}", query);
        try {
            stm = conn.prepareStatement(query);
            return stm.executeUpdate();
        } catch (SQLException e) {
            LOG.error(e, "Error during sql query");
            throw new IllegalStateException(e);
        } finally {
            try {
                if (stm != null) {
                    stm.close();
                }
            } catch (SQLException e) {
                LOG.error(e, "While closing sql statement");
            }
        }
    }

    public final ResultSet modifiedCsvFiles(final long syncToken) throws SQLException {
        final List<String> tableNames = fileToDB.createDbForSync(fileSystem.getModifiedCsvFiles(syncToken));

        tables.addAll(tableNames);

        return doQuery(conn.prepareStatement(query));
    }

    public ResultSet allCsvFiles() {
        final List<String> tableNames = fileToDB.createDbForSync(fileSystem.getAllCsvFiles());
        tables.addAll(tableNames);

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            return doQuery(stmt);
        } catch (SQLException ex) {
            LOG.error(ex, "Error during sql query");
            throw new IllegalStateException(ex);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    LOG.error(ex, "While closing sql statement");
                }
            }
        }
    }

    public ResultSet allCsvFiles(final String where, final List<SQLParam> params) {
        final List<String> tableNames = fileToDB.createDbForSync(fileSystem.getAllCsvFiles());
        tables.addAll(tableNames);

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(
                    query + (where != null && !where.isEmpty()
                    ? " WHERE " + where : ""));

            SQLUtil.setParams(stmt, params);

            return doQuery(stmt);
        } catch (SQLException e) {
            LOG.error(e, "Error during sql query");
            throw new IllegalStateException(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException ex) {
                LOG.error(ex, "While closing sql statement");
            }
        }
    }

    private ResultSet doQuery(final PreparedStatement stm)
            throws SQLException {

        LOG.ok("Execute query {0}", stm.toString());
        return stm.executeQuery();
    }

    private void dropTableAndViewIfExists()
            throws SQLException {

        LOG.ok("Drop view {0}", viewname);

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("DROP VIEW " + viewname + " IF EXISTS CASCADE");
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }

        for (String table : tables) {
            LOG.ok("Drop table {0}", table);

            try {
                stmt = conn.createStatement();
                stmt.execute("DROP TABLE " + table + " IF EXISTS CASCADE");
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        }
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public String getViewname() {
        return viewname;
    }

    public Connection getConn() {
        return conn;
    }

    public CSVDirConfiguration getConf() {
        return conf;
    }

    public Set<String> getTables() {
        return tables;
    }
}
