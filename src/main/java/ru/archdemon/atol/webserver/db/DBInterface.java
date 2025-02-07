package ru.archdemon.atol.webserver.db;

import java.util.List;
import ru.archdemon.atol.webserver.entities.BlockRecord;
import ru.archdemon.atol.webserver.entities.Device;
import ru.archdemon.atol.webserver.entities.Result;
import ru.archdemon.atol.webserver.entities.Request;
import ru.archdemon.atol.webserver.entities.RequestsQueueStatus;
import ru.archdemon.atol.webserver.entities.Setting;

public interface DBInterface {

    void init() throws DBException;

    void addTask(Request paramTask) throws DBException;

    Request getNextTask(String deviceId) throws DBException;

    Request getTask(String paramString) throws DBException;

    void setTaskReady(String paramString) throws DBException;

    List<Result> getTaskStatus(Long requestId) throws DBException;

    void updateSubTaskStatus(Result paramSubtaskStatus) throws DBException;

    void blockDB(BlockRecord paramBlockRecord) throws DBException;

    BlockRecord getBlockState(String deviceId) throws DBException;

    void unblockDB(String deviceId) throws DBException;

    RequestsQueueStatus getTasksStat(String deviceId) throws DBException;

    void cancelTask(String paramString) throws DBException;

    void addDevice(Device device) throws DBException;

    void updateDevice(String id, Device device) throws DBException;

    void deleteDevice(String id) throws DBException;

    void defaultDevice(String id) throws DBException;

    String defaultDevice() throws DBException;

    List<Device> getDevices() throws DBException;

    Device getDevice(String id) throws DBException, NotFoundException;

    Setting getSetting() throws DBException;

    void updateSetting(Setting setting) throws DBException;
}
