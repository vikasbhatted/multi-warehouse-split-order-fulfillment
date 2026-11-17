package com.fashionhub.fulfillment.web;

import com.fashionhub.fulfillment.dto.TaskViews.ClaimRequest;
import com.fashionhub.fulfillment.dto.TaskViews.CompleteTaskRequest;
import com.fashionhub.fulfillment.dto.TaskViews.TaskView;
import com.fashionhub.fulfillment.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Custom Tasklist API (inbox, claim, complete) on top of Camunda user tasks. */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    public List<TaskView> list(@RequestParam(required = false) String group,
                               @RequestParam(required = false) String assignee,
                               @RequestParam(defaultValue = "false") boolean unassignedOnly) {
        return tasks.list(group, assignee, unassignedOnly);
    }

    @GetMapping("/{key}")
    public TaskView get(@PathVariable String key) {
        return tasks.get(key);
    }

    @PostMapping("/{key}/claim")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void claim(@PathVariable String key, @Valid @RequestBody ClaimRequest body) {
        tasks.claim(key, body.assignee());
    }

    @PostMapping("/{key}/unclaim")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unclaim(@PathVariable String key, @Valid @RequestBody ClaimRequest body) {
        tasks.unclaim(key, body.assignee());
    }

    @PostMapping("/{key}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable String key, @Valid @RequestBody CompleteTaskRequest body) {
        tasks.complete(key, body.assignee(), body.variables());
    }
}
