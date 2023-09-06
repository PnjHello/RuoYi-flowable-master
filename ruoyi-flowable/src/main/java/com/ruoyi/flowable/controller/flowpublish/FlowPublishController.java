package com.ruoyi.flowable.controller.flowpublish;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.flowable.domain.dto.flowpublish.*;
import com.ruoyi.flowable.service.IFlowPublishService;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "工作流接口发布管理")
@RestController
@RequestMapping("/flowable/public")
public class FlowPublishController {
    @Autowired
    private IFlowPublishService flowPublishService;

    @ApiOperation(value = "根据流程定义id启动流程实例，返回流程实例id")
    @PostMapping("/startBy")
    public R<ProcessInstanceDto> startById(@RequestBody FlowStartDto flowStartDto) {
        try {
            return R.ok(flowPublishService.startProcessInstanceById(flowStartDto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation(value = "删除流程实例")
    @DeleteMapping(value = "/deleteProcessInstance/{instanceIds}")
    public R deleteProcessInstance(@ApiParam(value = "流程实例Id，多个流程实例使用逗号（,）分隔", required = true) @PathVariable String[] instanceIds,
                                   @ApiParam(value = "删除原因") @RequestParam(required = false) String deleteReason) {
        for (String instanceId : instanceIds) {
            flowPublishService.delete(instanceId, deleteReason);
        }
        return R.ok();
    }

    @ApiOperation(value = "获取下一节点")
    @PostMapping(value = "/nextFlowNode/{processInstanceId}/{userId}")
    public R<List<NextFlowDto>> getNextFlowNode(@ApiParam(value = "流程实例Id") @PathVariable(value = "processInstanceId") String processInstanceId, @ApiParam(value = "任务所属人id") @PathVariable(value = "userId") String userId) {
        try {
            return R.ok(flowPublishService.getNextFlowNode(processInstanceId, null, userId));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation(value = "审批任务")
    @PostMapping(value = "/complete")
    public R complete(@RequestBody TaskCompleteDto taskCompleteDto) {
        try {
            flowPublishService.complete(taskCompleteDto);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation(value = "退回任务")
    @PostMapping(value = "/return")
    public R taskReturn(@RequestBody TaskReturnDto taskReturnDto) {
        try {
            flowPublishService.taskReturn(taskReturnDto);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation(value = "获取待办列表", response = FlowTodoDto.class)
    @GetMapping(value = "/todoList")
    public R<List<FlowTodoDto>> todoList(
            @ApiParam(value = "当前页码", required = true) Integer pageNum, @ApiParam(value = "每页条数", required = true) Integer pageSize,
            @ApiParam(value = "待办人Id", required = true) String userId) {
        return R.ok(flowPublishService.todoList(pageNum, pageSize, userId).getRecords());
    }

    @ApiOperation(value = "流程历史流转记录", response = FlowRecordDto.class)
    @GetMapping(value = "/flowRecord")
    public R<List<FlowRecordDto>> flowRecord(@ApiParam(value = "流程实例Id", required = true) String processInstanceId) {
        return R.ok(flowPublishService.flowRecord(processInstanceId));
    }

}
