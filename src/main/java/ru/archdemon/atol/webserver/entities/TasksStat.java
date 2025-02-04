package ru.archdemon.atol.webserver.entities;

public class TasksStat {

    private int tasksReadyCount;
    private int tasksNotReadyCount;
    private int tasksCanceledCount;

    public int getTasksNotReadyCount() {
        return this.tasksNotReadyCount;
    }

    public void setTasksNotReadyCount(int tasksNotReadyCount) {
        this.tasksNotReadyCount = tasksNotReadyCount;
    }

    public int getTasksCanceledCount() {
        return this.tasksCanceledCount;
    }

    public void setTasksCanceledCount(int tasksCanceledCount) {
        this.tasksCanceledCount = tasksCanceledCount;
    }

    public int getTasksReadyCount() {
        return this.tasksReadyCount;
    }

    public void setTasksReadyCount(int tasksReadyCount) {
        this.tasksReadyCount = tasksReadyCount;
    }
}
