package ru.archdemon.atol.webserver.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.sqlite.SQLiteErrorCode;
import ru.archdemon.atol.webserver.entities.*;

public class SQLiteDB implements DBInterface {

    private static final int CURRENT_VERSION = 3;

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

    private void executeMigration(String filename, Connection connection) throws SQLException, IOException {
        Path path = new File(getClass().getResource(filename).getFile()).toPath();
        String content = new String(Files.readAllBytes(path));
        for (String sql : content.split("\n\n|\r\n\r\n")) {
            if (!sql.trim().isBlank()) {
                Statement stmt = connection.createStatement();
                stmt.execute(sql);
            }
        }
    }

    @Override
    public void init() throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            Statement stmt = connection.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS meta ([key] VARCHAR (10) PRIMARY KEY NOT NULL, value TEXT);");

            ResultSet versionQueryResult = stmt.executeQuery("SELECT value FROM meta WHERE key = 'version';");

            String version = "";
            while (versionQueryResult.next()) {
                version = versionQueryResult.getString("value");
            }

            if (version.isEmpty()) {
                version = "1";
                executeMigration("/migrations/v1.sql", connection);
            }

            while (Integer.parseInt(version) < CURRENT_VERSION) {
                version = String.valueOf(convertBD(Integer.parseInt(version), connection));
            }

            if (Integer.parseInt(version) != CURRENT_VERSION) {
                throw new DBException("Неверная версия БД");
            }

        } catch (NumberFormatException | SQLException | IOException e) {
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

    private int convertBD(int fromVersion, Connection connection) throws SQLException, IOException {
        switch (fromVersion) {
            case 1:
                executeMigration("/migrations/v2.sql", connection);
                updateDBVersion(2, connection);
                return 2;
            case 2:
                executeMigration("/migrations/v3.sql", connection);
                updateDBVersion(3, connection);
                return 3;
            default:
                throw new IndexOutOfBoundsException(String.format("Unsupported version '%s'", fromVersion));
        }
    }

    @Override
    public void addTask(Request task) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            connection.setAutoCommit(false);

            PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO requests (uuid, data, created_time, device_id) VALUES (?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, task.getUuid());
            pstmt.setString(2, task.getData());
            pstmt.setObject(3, task.getCreatedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            pstmt.setObject(4, task.getDeviceId());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long id = generatedKeys.getLong(1);
                        for (int i = 0; i < task.getSubTaskCount(); i++) {
                            pstmt = connection.prepareStatement(
                                    "INSERT INTO results (uuid, number, created_time, request_id) VALUES (?, ?, ?, ?)");

                            pstmt.setString(1, task.getUuid());
                            pstmt.setInt(2, i);
                            pstmt.setObject(3, task.getCreatedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                            pstmt.setLong(4, id);
                            pstmt.executeUpdate();
                        }
                    } else {
                        throw new DBException("Failed to retrieve auto-generated keys.");
                    }
                }
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
    public Request getNextTask(String deviceId) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM requests WHERE device_id = ? AND is_ready != 1 AND is_canceled != 1 ORDER BY id LIMIT 1");
            stmt.setString(1, deviceId);

            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                Request task = new Request();
                task.setUuid(result.getString("uuid"));
                task.setData(result.getString("data"));

                return task;
            }

            return null;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Request getTask(String uuid) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM requests WHERE uuid = ?");

            pstmt.setString(1, uuid);
            ResultSet result = pstmt.executeQuery();

            Request task = new Request();
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
        } catch (SQLException e) {
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
            PreparedStatement pstmt = connection.prepareStatement("UPDATE requests SET is_ready = ? WHERE uuid = ?");

            pstmt.setBoolean(1, true);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<Result> getTaskStatus(Long requestId) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT * FROM results WHERE request_id = ? ORDER BY id");

            pstmt.setLong(1, requestId);
            ResultSet result = pstmt.executeQuery();

            List<Result> status = new LinkedList<>();
            while (result.next()) {
                Result s = new Result();
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
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void updateSubTaskStatus(Result status) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement(
                    "UPDATE results SET status = ?, error_code = ?, error_description = ?, result_data = ?, updated_time = ?"
                    + " WHERE request_id = ? AND number = ?");

            pstmt.setInt(1, status.getStatus());
            pstmt.setInt(2, status.getErrorCode());
            pstmt.setString(3, status.getErrorDescription());
            pstmt.setString(4, status.getResultData());
            pstmt.setObject(5, new Date());
            pstmt.setLong(6, status.getRequestId());
            pstmt.setInt(7, status.getNumber());
            pstmt.executeUpdate();
        } catch (SQLException e) {
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
            PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT OR REPLACE INTO block_records (uuid, device_id, created_time, data) VALUES (?, ?, ?, ?)");

            pstmt.setString(1, block.getUuid());
            pstmt.setString(2, block.getDeviceId());
            pstmt.setObject(3, new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            pstmt.setString(4, block.getData());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public BlockRecord getBlockState(String deviceId) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM block_records WHERE device_id = ?");
            stmt.setString(1, deviceId);
            ResultSet result = stmt.executeQuery();

            BlockRecord block = null;
            while (result.next()) {
                block = new BlockRecord();
                block.setId(result.getLong("id"));
                block.setUuid(result.getString("uuid"));
                block.setDeviceId(result.getString("device_id"));
                block.setData(result.getString("data"));
                block.setPaperError(result.getBoolean("is_paper_error"));
                block.setFnError(result.getBoolean("is_fn_error"));
                block.setConnectionError(result.getBoolean("is_connection_error"));
                block.setCreatedTime(result.getDate("created_time"));
            }

            return block;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void unblockDB(String deviceId) throws DBException {
        Connection connection = null;
        try {
            connection = connect();

            PreparedStatement pstmt = connection.prepareStatement("DELETE FROM block_records WHERE device_id = ?");
            pstmt.setString(1, deviceId);

            pstmt.execute();

        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public RequestsQueueStatus getTasksStat(String deviceId) throws DBException {
        RequestsQueueStatus stat = new RequestsQueueStatus();
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM requests WHERE device_id = ? AND is_canceled = 1");
            stmt.setString(1, deviceId);

            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                stat.setCanceled(result.getInt(1));
            }

            stmt = connection.prepareStatement("SELECT is_ready, COUNT(*) FROM requests WHERE device_id = ? GROUP BY is_ready");
            stmt.setString(1, deviceId);

            result = stmt.executeQuery();
            while (result.next()) {
                if (result.getBoolean(1)) {
                    stat.setReady(result.getInt(2) - stat.getCanceled());
                    continue;
                }
                stat.setNumber(result.getInt(2));
            }

            return stat;
        } catch (SQLException e) {
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
            PreparedStatement pstmt = connection.prepareStatement(
                    "UPDATE requests SET is_ready = ?, is_canceled = ? WHERE uuid = ?");

            pstmt.setBoolean(1, true);
            pstmt.setBoolean(2, true);
            pstmt.setString(3, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void updateDevice(String id, Device device) throws DBException {
        Connection connection = null;
        try {
            connection = connect();

            PreparedStatement pstmt = connection.prepareStatement(
                    "UPDATE devices SET name = ?, is_active = ?, is_default = ?, model = ?, user_password = ?"
                    + ", access_password = ?, port = ?, com = ?, usb = ?, baud_rate = ?"
                    + ", ip_addr = ?, ip_port = ?, mac = ?, ofd_channel = ?, use_global_sp = ?,"
                    + " scripts_path = ?, use_global_icds = ?, invert_cd_status = ?, use_global_hl = ?"
                    + ", header_lines = ?, use_global_fl = ?, footer_lines = ?, id = ?"
                    + " WHERE id = ?");

            pstmt.setString(1, device.getName());
            pstmt.setBoolean(2, device.isActive());
            pstmt.setBoolean(3, device.isDefault());
            pstmt.setInt(4, device.getModel());
            pstmt.setString(5, device.getUserPassword());
            pstmt.setString(6, device.getAccessPassword());
            pstmt.setString(7, device.getPort());
            pstmt.setString(8, device.getCom());
            pstmt.setString(9, device.getUsb());
            pstmt.setInt(10, device.getBaudRate());
            pstmt.setString(11, device.getIpAddr());
            pstmt.setInt(12, device.getIpPort());
            pstmt.setString(13, device.getMac());
            pstmt.setString(14, device.getOfdChannel());
            pstmt.setBoolean(15, device.isUseGlobalSp());
            pstmt.setString(16, device.getScriptsPath());
            pstmt.setBoolean(17, device.isUseGlobalIcds());
            pstmt.setBoolean(18, device.isInvertCdStatus());
            pstmt.setBoolean(19, device.isUseGlobalHl());
            pstmt.setString(20, device.getHeaderLines());
            pstmt.setBoolean(21, device.isUseGlobalFl());
            pstmt.setString(22, device.getFooterLines());
            pstmt.setString(23, device.getId());
            pstmt.setString(24, id);

            pstmt.execute();
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void deleteDevice(String id) throws DBException {
        Connection connection = null;
        try {
            connection = connect();

            PreparedStatement pstmt = connection.prepareStatement("DELETE FROM devices WHERE id = ? ");
            pstmt.setString(1, id);

            pstmt.execute();
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void addDevice(Device device) throws DBException {
        Connection connection = null;
        try {
            connection = connect();

            PreparedStatement pstmt = connection.prepareStatement("INSERT INTO devices (id, name) VALUES (?, ?)");

            pstmt.setString(1, device.getId());
            pstmt.setString(2, device.getName());

            pstmt.execute();
        } catch (SQLException e) {
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CONSTRAINT.code) {
                throw new NotUniqueKeyException(e);
            }
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void defaultDevice(String id) throws DBException {
        Connection connection = null;
        try {
            connection = connect();

            Statement stmt = connection.createStatement();
            stmt.execute("UPDATE devices SET is_default = 0");

            PreparedStatement pstmt = connection.prepareStatement("UPDATE devices SET is_default = 1 WHERE id = ?");
            pstmt.setString(1, id);

            pstmt.execute();
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public String defaultDevice() throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM devices WHERE is_default = ?");
            stmt.setBoolean(1, true);
            ResultSet result = stmt.executeQuery();

            while (result.next()) {
                return result.getString("id");
            }

            return null;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<Device> getDevices() throws DBException {
        List<Device> devices = new ArrayList<>();
        Connection connection = null;
        try {
            connection = connect();
            Statement stmt = connection.createStatement();
            ResultSet result = stmt.executeQuery("SELECT * FROM devices");
            while (result.next()) {
                Device device = new Device(result.getString("id"));
                device.setName(result.getString("name"));
                device.setActive(result.getBoolean("is_active"));
                device.setDefault(result.getBoolean("is_default"));
                device.setModel(result.getInt("model"));
                device.setUserPassword(result.getString("user_password"));
                device.setAccessPassword(result.getString("access_password"));
                device.setPort(result.getString("port"));
                device.setCom(result.getString("com"));
                device.setUsb(result.getString("usb"));
                device.setBaudRate(result.getInt("baud_rate"));
                device.setIpAddr(result.getString("ip_addr"));
                device.setIpPort(result.getInt("ip_port"));
                device.setMac(result.getString("mac"));
                device.setOfdChannel(result.getString("ofd_channel"));
                device.setUseGlobalSp(result.getBoolean("use_global_sp"));
                device.setScriptsPath(result.getString("scripts_path"));
                device.setUseGlobalIcds(result.getBoolean("use_global_icds"));
                device.setInvertCdStatus(result.getBoolean("invert_cd_status"));
                device.setUseGlobalHl(result.getBoolean("use_global_hl"));
                device.setHeaderLines(result.getString("header_lines"));
                device.setUseGlobalFl(result.getBoolean("use_global_fl"));
                device.setFooterLines(result.getString("footer_lines"));
                device.setBlockId(result.getLong("block_id"));

                devices.add(device);
            }
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }

        return devices;
    }

    @Override
    public Device getDevice(String id) throws DBException {
        Connection connection = null;
        try {
            connection = connect();

            if (id == null) {
                Statement stmt = connection.createStatement();
                ResultSet result = stmt.executeQuery("SELECT id FROM devices WHERE is_default = 1");
                while (result.next()) {
                    id = result.getString("id");
                }
            }

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM devices WHERE id = ?");
            stmt.setString(1, id);

            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                Device device = new Device(result.getString("id"));
                device.setName(result.getString("name"));
                device.setActive(result.getBoolean("is_active"));
                device.setDefault(result.getBoolean("is_default"));
                device.setModel(result.getInt("model"));
                device.setUserPassword(result.getString("user_password"));
                device.setAccessPassword(result.getString("access_password"));
                device.setPort(result.getString("port"));
                device.setCom(result.getString("com"));
                device.setUsb(result.getString("usb"));
                device.setBaudRate(result.getInt("baud_rate"));
                device.setIpAddr(result.getString("ip_addr"));
                device.setIpPort(result.getInt("ip_port"));
                device.setMac(result.getString("mac"));
                device.setOfdChannel(result.getString("ofd_channel"));
                device.setUseGlobalSp(result.getBoolean("use_global_sp"));
                device.setScriptsPath(result.getString("scripts_path"));
                device.setUseGlobalIcds(result.getBoolean("use_global_icds"));
                device.setInvertCdStatus(result.getBoolean("invert_cd_status"));
                device.setUseGlobalHl(result.getBoolean("use_global_hl"));
                device.setHeaderLines(result.getString("header_lines"));
                device.setUseGlobalFl(result.getBoolean("use_global_fl"));
                device.setFooterLines(result.getString("footer_lines"));
                device.setBlockId(result.getLong("block_id"));

                return device;
            }
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }

        throw new NotFoundException("Device not found");
    }

    @Override
    public Setting getSetting() throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM global_settings");

            ResultSet result = stmt.executeQuery();
            while (result.next()) {
                Setting setting = new Setting();
                setting.setScriptsPath(result.getString("scripts_path"));
                setting.setInvertCdStatus(result.getBoolean("invert_cd_status"));
                setting.setHeaderLines(result.getString("header_lines"));
                setting.setFooterLines(result.getString("footer_lines"));
                setting.setBlockOnPrintErrors(result.getBoolean("block_on_print_errors"));
                setting.setDeleteRequestsAfter(result.getInt("delete_requests_after"));
                setting.setValidateRequestsOnAdd(result.getBoolean("validate_requests_on_add"));

                return setting;
            }
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }

        return null;
    }

    @Override
    public void updateSetting(Setting setting) throws DBException {
        Connection connection = null;
        try {
            connection = connect();
            PreparedStatement pstmt = connection.prepareStatement(
                    "UPDATE global_settings SET scripts_path = ?, invert_cd_status = ?, "
                    + "header_lines = ?, footer_lines = ?, block_on_print_errors = ?, "
                    + "delete_requests_after = ?, validate_requests_on_add = ?");

            pstmt.setString(1, setting.getScriptsPath());
            pstmt.setBoolean(2, setting.isInvertCdStatus());
            pstmt.setString(3, setting.getHeaderLines());
            pstmt.setString(4, setting.getFooterLines());
            pstmt.setBoolean(5, setting.isBlockOnPrintErrors());
            pstmt.setInt(6, setting.getDeleteRequestsAfter());
            pstmt.setBoolean(7, setting.isValidateRequestsOnAdd());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            closeConnection(connection);
        }
    }

}
