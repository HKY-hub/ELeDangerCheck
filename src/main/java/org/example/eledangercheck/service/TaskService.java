package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.Task;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.TaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskMapper taskMapper;

    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public Task createTask(Task task) {
        task.setCreateTime(LocalDateTime.now());
        if (task.getStatus() == null) {
            task.setStatus("pending");
        }
        taskMapper.insert(task);
        return task;
    }

    public Task getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    public List<Task> getAllTasks() {
        return taskMapper.selectList(null);
    }

    public List<Task> getTasksByPrincipal(Long principal) {
        return taskMapper.selectByPrincipalId(principal);
    }

    public List<Task> getTasksByStatus(String status) {
        return taskMapper.selectByStatus(status);
    }

    public Task updateTask(Long id, Task taskDetails) {
        Task task = getTaskById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (taskDetails.getTaskName() != null) {
            task.setTaskName(taskDetails.getTaskName());
        }
        if (taskDetails.getTaskType() != null) {
            task.setTaskType(taskDetails.getTaskType());
        }
        if (taskDetails.getTaskDesc() != null) {
            task.setTaskDesc(taskDetails.getTaskDesc());
        }
        if (taskDetails.getVoltageLevel() != null) {
            task.setVoltageLevel(taskDetails.getVoltageLevel());
        }
        if (taskDetails.getEquipmentType() != null) {
            task.setEquipmentType(taskDetails.getEquipmentType());
        }
        if (taskDetails.getWorkType() != null) {
            task.setWorkType(taskDetails.getWorkType());
        }
        if (taskDetails.getEnvConditions() != null) {
            task.setEnvConditions(taskDetails.getEnvConditions());
        }
        if (taskDetails.getStatus() != null) {
            task.setStatus(taskDetails.getStatus());
        }
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        return task;
    }

    public void deleteTask(Long id) {
        if (taskMapper.selectById(id) == null) {
            throw new BusinessException("任务不存在");
        }
        taskMapper.deleteById(id);
    }
}