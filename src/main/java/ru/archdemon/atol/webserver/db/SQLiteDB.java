package ru.archdemon.atol.webserver.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import org.sqlite.SQLiteErrorCode;
import ru.archdemon.atol.webserver.entities.BlockRecord;
import ru.archdemon.atol.webserver.entities.SubtaskStatus;
import ru.archdemon.atol.webserver.entities.Task;
import ru.archdemon.atol.webserver.entities.TasksStat;

public class SQLiteDB implements DBInterface {

    private static final int CURRENT_VERSION = 2;

    private Connection connect() throws DBException {
        try {
            Class.forName("org.sqlite.JDBC");

            File dir = new File(System.getProperty("db.directory"));
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return DriverManager.getConnection(String.format("jdbc:sqlite:%s/web.s3db", dir.getAbsolutePath()));
        } catch (ClassNotFoundException | SQLException e) {
            throw new DBException(e);
        }
    }

    private void closeConnection(Connection c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException sQLException) {
        }
    }

    private void rollback(Connection c) {
        try {
            if (c != null) {
                c.rollback();
            }
        } catch (SQLException sQLException) {
        }
    }

    @Override
    public void init() throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            Statement stmt = connection.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS meta (   [key] VARCHAR (10) PRIMARY KEY                      NOT NULL,   value TEXT);");

            ResultSet versionQueryResult = stmt.executeQuery("SELECT value FROM meta WHERE key = 'version';");

            String version = "";
            while (versionQueryResult.next()) {
                version = versionQueryResult.getString("value");
            }

            if (version.isEmpty()) {
                version = String.valueOf(CURRENT_VERSION);
                PreparedStatement pstmt = connection.prepareStatement("INSERT INTO meta (key, value) VALUES (?, ?)");
                pstmt.setString(1, "version");
                pstmt.setString(2, version);
                pstmt.executeUpdate();
            }

            if (!version.isEmpty() && Integer.parseInt(version) < CURRENT_VERSION) {
                version = String.valueOf(convertBD(Integer.parseInt(version), connection));
            }

            if (Integer.parseInt(version) != CURRENT_VERSION) {
                throw new DBException("Неверная версия БД");
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS json_tasks (   uuid      VARCHAR (36) PRIMARY KEY                          NOT NULL,   data      TEXT,   is_ready  BOOLEAN DEFAULT 0,   is_canceled  BOOLEAN DEFAULT 0,   timestamp DATETIME);");

            stmt.execute("CREATE TABLE IF NOT EXISTS json_results (   uuid              VARCHAR (36) NOT NULL,   number            INTEGER      NOT NULL,   timestamp         DATETIME,   status            INTEGER DEFAULT 0,    error_code        INTEGER DEFAULT 0,    error_description TEXT DEFAULT '',   result_data       TEXT DEFAULT '',   PRIMARY KEY (       uuid,       number\t));");

            try {
                stmt.execute("CREATE TABLE settings (\tsetting   VARCHAR (32) PRIMARY KEY                           NOT NULL,   value     TEXT);");

                PreparedStatement pstmt = connection.prepareStatement("INSERT INTO settings (setting, value) VALUES (?, ?)");
                pstmt.setString(1, "clear_interval");
                pstmt.setString(2, "720");
                pstmt.executeUpdate();
            } catch (SQLException exception) {
            }

            try {
                stmt.execute("CREATE TABLE state (\tstate_id  VARCHAR (32) PRIMARY KEY                           NOT NULL,   value     TEXT);");

            } catch (SQLException exception) {
            }

            stmt.execute("CREATE TRIGGER IF NOT EXISTS clear_tasks BEFORE INSERT ON json_tasks BEGIN \tDELETE FROM json_tasks WHERE \t((JULIANDAY('now', 'localtime') - JULIANDAY(timestamp)) * 24 >= (\t\tSELECT CAST(value as INTEGER) FROM settings WHERE setting = 'clear_interval')\t);END;");

            stmt.execute("CREATE TRIGGER IF NOT EXISTS clear_results BEFORE INSERT ON json_results BEGIN \tDELETE FROM json_results WHERE \t((JULIANDAY('now', 'localtime') - JULIANDAY(timestamp)) * 24 >= (\t\tSELECT CAST(value as INTEGER) FROM settings WHERE setting = 'clear_interval')\t);END;");

        } catch (NumberFormatException | SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    private void updateDBVersion(int version, Connection connection) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement("UPDATE meta SET value = ? WHERE key = 'version'");
        pstmt.setString(1, String.valueOf(version));
        pstmt.executeUpdate();
    }

    private int convertBD(int fromVersion, Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();

        switch (fromVersion) {
            case 1:
                stmt.execute("ALTER TABLE json_tasks ADD is_canceled BOOLEAN DEFAULT 0");
                updateDBVersion(CURRENT_VERSION, connection);
                break;
        }
        return CURRENT_VERSION;
    }

    @Override
    public void addTask(Task task) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            connection.setAutoCommit(false);

            PreparedStatement pstmt = connection.prepareStatement("INSERT INTO json_tasks (uuid, data, is_ready, timestamp) VALUES (?, ?, 0, ?)");

            pstmt.setString(1, task.getUuid());
            pstmt.setString(2, task.getData());
            pstmt.setObject(3, task.getTimestamp().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
            pstmt.executeUpdate();

            for (int i = 0; i < task.getSubTaskCount(); i++) {
                pstmt = connection.prepareStatement("INSERT INTO json_results (uuid, number, timestamp) VALUES (?, ?, ?)");

                pstmt.setString(1, task.getUuid());
                pstmt.setInt(2, i);
                pstmt.setObject(3, task.getTimestamp().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime());
                pstmt.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            rollback(connection);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CONSTRAINT.code) {
                throw new NotUniqueKeyException(e);
            }
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Task getNextTask() throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            Statement stmt = connection.createStatement();
            ResultSet result = stmt.executeQuery("SELECT uuid, data, MIN(timestamp) FROM json_tasks WHERE is_ready != 1 AND is_canceled != 1");

            Task task = new Task();
            while (result.next()) {
                task.setUuid(result.getString("uuid"));
                task.setData(result.getString("data"));
            }

            if (task.getUuid() == null) {
                return null;
            }
            return task;
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Task getTask(String uuid) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement("SELECT uuid, data, is_ready, is_canceled FROM json_tasks WHERE uuid == ?");

            pstmt.setString(1, uuid);
            ResultSet result = pstmt.executeQuery();

            Task task = new Task();
            while (result.next()) {
                task.setUuid(result.getString("uuid"));
                task.setData(result.getString("data"));
                task.setReady(result.getBoolean("is_ready"));
                task.setCanceled(result.getBoolean("is_canceled"));
            }

            if (task.getUuid() == null) {
                return null;
            }
            return task;
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void setTaskReady(String uuid) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement("UPDATE json_tasks SET is_ready = ? WHERE uuid = ?");

            pstmt.setBoolean(1, true);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<SubtaskStatus> getTaskStatus(String uuid) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement("SELECT status, error_code, error_description, result_data FROM json_results WHERE uuid = ? ORDER BY number");

            pstmt.setString(1, uuid);
            ResultSet result = pstmt.executeQuery();

            List<SubtaskStatus> status = new LinkedList<>();
            while (result.next()) {
                SubtaskStatus s = new SubtaskStatus();
                s.setStatus(result.getInt("status"));
                s.setErrorCode(result.getInt("error_code"));
                s.setErrorDescription(result.getString("error_description"));
                s.setResultData(result.getString("result_data"));
                status.add(s);
            }

            if (status.isEmpty()) {
                return null;
            }
            return status;
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void updateSubTaskStatus(String uuid, int number, SubtaskStatus status) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement("UPDATE json_results SET  status = ?, error_code = ?, error_description = ?, result_data = ? WHERE uuid = ? AND number = ?");

            pstmt.setInt(1, status.getStatus());
            pstmt.setInt(2, status.getErrorCode());
            pstmt.setString(3, status.getErrorDescription());
            pstmt.setString(4, status.getResultData());
            pstmt.setString(5, uuid);
            pstmt.setInt(6, number);
            pstmt.executeUpdate();
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void blockDB(BlockRecord block) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement("INSERT OR REPLACE INTO state (state_id, value) VALUES (?, ?)");

            pstmt.setString(1, "block_uuid");
            pstmt.setString(2, block.getUuid());
            pstmt.executeUpdate();

            pstmt = connection.prepareStatement("INSERT OR REPLACE INTO state (state_id, value) VALUES (?, ?)");

            pstmt.setString(1, "block_last_fd");
            pstmt.setString(2, String.valueOf(block.getDocumentNumber()));
            pstmt.executeUpdate();
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public BlockRecord getBlockState() throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            Statement stmt = connection.createStatement();
            ResultSet result = stmt.executeQuery("SELECT state_id, value FROM state");

            String blockUUID = "", blockDocument = "-1";
            while (result.next()) {
                String key = result.getString("state_id");
                String value = result.getString("value");
                switch (key) {
                    case "block_uuid":
                        blockUUID = value;

                    case "block_last_fd":
                        blockDocument = value;
                }

            }
            return new BlockRecord(blockUUID, Long.parseLong(blockDocument));
        } catch (NumberFormatException | SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void unblockDB() throws DBException {
        Connection connection = null;
        try {
            connection = connect();

            PreparedStatement pstmt = connection.prepareStatement("DELETE FROM state WHERE state_id = ?");

            pstmt.setString(1, "block_uuid");
            pstmt.execute();

            pstmt = connection.prepareStatement("DELETE FROM state WHERE state_id = ?");

            pstmt.setString(1, "block_last_fd");
            pstmt.execute();
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public TasksStat getTasksStat() throws DBException {
        TasksStat stat = new TasksStat();
        Connection connection = null;
        try {
            connection = connect();
            Statement stmt = connection.createStatement();

            ResultSet result = stmt.executeQuery("SELECT COUNT(*) FROM json_tasks WHERE is_canceled = 1");

            int canceled = 0;
            while (result.next()) {
                canceled = result.getInt(1);
                stat.setTasksCanceledCount(canceled);
            }

            result = stmt.executeQuery("SELECT is_ready, COUNT(*) FROM json_tasks GROUP BY is_ready");

            while (result.next()) {
                if (result.getBoolean(1)) {
                    stat.setTasksReadyCount(result.getInt(2) - canceled);
                    continue;
                }
                stat.setTasksNotReadyCount(result.getInt(2));
            }

            return stat;
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void cancelTask(String uuid) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement("UPDATE json_tasks SET is_ready = ?, is_canceled = ? WHERE uuid = ?");

            pstmt.setBoolean(1, true);
            pstmt.setBoolean(2, true);
            pstmt.setString(3, uuid);
            pstmt.executeUpdate();
        } catch (SQLException | DBException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }
}
