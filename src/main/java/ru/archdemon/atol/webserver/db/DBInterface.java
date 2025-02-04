package ru.archdemon.atol.webserver.db;

import java.util.List;
import ru.archdemon.atol.webserver.entities.BlockRecord;
import ru.archdemon.atol.webserver.entities.SubtaskStatus;
import ru.archdemon.atol.webserver.entities.Task;
import ru.archdemon.atol.webserver.entities.TasksStat;

public interface DBInterface {

    void init() throws DBException;

    void addTask(Task paramTask) throws DBException;

    Task getNextTask() throws DBException;

    Task getTask(String paramString) throws DBException;

    void setTaskReady(String paramString) throws DBException;

    List<SubtaskStatus> getTaskStatus(String paramString) throws DBException;

    void updateSubTaskStatus(String paramString, int paramInt, SubtaskStatus paramSubtaskStatus) throws DBException;

    void blockDB(BlockRecord paramBlockRecord) throws DBException;

    BlockRecord getBlockState() throws DBException;

    void unblockDB() throws DBException;

    TasksStat getTasksStat() throws DBException;

    void cancelTask(String paramString) throws DBException;
}
