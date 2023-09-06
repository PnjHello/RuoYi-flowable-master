package com.ruoyi.flowable.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.flowable.domain.dto.FlowTaskDto;
import com.ruoyi.flowable.domain.dto.flowpublish.*;
import com.ruoyi.flowable.domain.vo.FlowQueryVo;
import com.ruoyi.flowable.domain.vo.FlowTaskVo;
import org.flowable.engine.history.HistoricProcessInstance;

import java.util.List;
import java.util.Map;

public interface IFlowPublishService {
    ProcessInstanceDto startProcessInstanceById(FlowStartDto flowStartDto) throws Exception;

    void delete(String instanceId, String deleteReason);

    List<NextFlowDto> getNextFlowNode(String processInstanceId, String taskId, String userId);

    void complete(TaskCompleteDto taskCompleteDto) throws Exception;

    void taskReturn(TaskReturnDto taskReturnDto) throws Exception;

    Page<FlowTodoDto> todoList(Integer pageNum, Integer pageSize, String userId);

    List<FlowRecordDto> flowRecord(String processInstanceId);
}
