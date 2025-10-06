package com.intern.service;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CamundaProcessService {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    public String startProcess(String processKey, Map<String, Object> variables) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, variables);
        return processInstance.getProcessInstanceId();
    }

    public List<Task> getTasksForProcess(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
    }

    public void completeUserTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, variables);
    }

    public List<Task> getTasksForUser(String assignee) {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .list();
    }

    public List<Task> getUnassignedTasks() {
        return taskService.createTaskQuery()
                .taskUnassigned()
                .list();
    }

    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }
}