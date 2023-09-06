package com.ruoyi.flowable.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.flowable.common.constant.ProcessConstants;
import com.ruoyi.flowable.common.enums.FlowComment;
import com.ruoyi.flowable.domain.dto.FlowCommentDto;
import com.ruoyi.flowable.domain.dto.flowpublish.*;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.flowable.flow.FindNextNodeUtil;
import com.ruoyi.flowable.flow.FlowableUtils;
import com.ruoyi.flowable.service.IFlowPublishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FlowPublishService extends FlowServiceFactory implements IFlowPublishService {

    private String workUserParamName = "workUserId";

    private HistoricProcessInstance getHistoricProcessInstanceById(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance =
                historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (Objects.isNull(historicProcessInstance)) {
            throw new FlowableObjectNotFoundException("流程实例不存在: " + processInstanceId);
        }
        return historicProcessInstance;
    }

    /**
     * 根据流程定义ID启动流程实例
     *
     * @param flowStartDto
     * @return
     */
    @Override
    public ProcessInstanceDto startProcessInstanceById(FlowStartDto flowStartDto) throws Exception {
        if (!StringUtils.isNotBlank(flowStartDto.getProcDefId()))
            throw new Exception("流程定义ID不能为空");
        if (!StringUtils.isNotBlank(flowStartDto.getUserId()))
            throw new Exception("流程发起人ID不能为空");
        if (flowStartDto.getNextUserIds() == null || flowStartDto.getNextUserIds().size() == 0)
            throw new Exception("下一审核人不能为空");

        ProcessInstanceDto processInstanceDto = new ProcessInstanceDto();
        // 设置流程发起人Id到流程中
        identityService.setAuthenticatedUserId(flowStartDto.getUserId());
        if (flowStartDto.getVariables() == null)
            flowStartDto.setVariables(new HashMap<>());
        flowStartDto.getVariables().put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(flowStartDto.getProcDefId(), flowStartDto.getVariables());

        if (flowStartDto.getNextUserIds() != null && flowStartDto.getNextUserIds().size() > 0) {
            //设置下一步骤待办人
            List<Task> nextTaskList = taskService.createTaskQuery().active().includeProcessVariables().processInstanceId(processInstance.getProcessInstanceId()).list();
            if (nextTaskList.size() == 1)
                taskService.setAssignee(nextTaskList.get(0).getId(), String.join(",", flowStartDto.getNextUserIds()));
            else {
                for (int i = 0; i < nextTaskList.size(); i++) {
                    taskService.setAssignee(nextTaskList.get(i).getId(), flowStartDto.getNextUserIds().get(i));
                }
            }
        }

        processInstanceDto.processInstanceId = processInstance.getProcessInstanceId();

        return processInstanceDto;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String instanceId, String deleteReason) {
        // 查询历史数据
        HistoricProcessInstance historicProcessInstance = getHistoricProcessInstanceById(instanceId);
        if (historicProcessInstance.getEndTime() != null) {
            historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
            return;
        }
        // 删除流程实例
        runtimeService.deleteProcessInstance(instanceId, deleteReason);
        // 删除历史流程实例
        historyService.deleteHistoricProcessInstance(instanceId);
    }

    /**
     * 获取下一节点
     *
     * @param processInstanceId 流程实例id
     * @param taskId            任务id
     * @param userId            任务所属人id
     * @return
     */
    @Override
    public List<NextFlowDto> getNextFlowNode(String processInstanceId, String taskId, String userId) {
        // Step 1. 获取当前节点并找到下一步节点
        Task task = null;
        if (StringUtils.isNotBlank(processInstanceId) && StringUtils.isNotBlank(userId) && !StringUtils.isNotBlank(taskId))
            task = taskService.createTaskQuery().active().includeProcessVariables().processInstanceId(processInstanceId).taskAssigneeLike("%" + userId + "%").list().stream().findFirst().orElse(null);

        if (StringUtils.isNotBlank(taskId))
            task = taskService.createTaskQuery().taskId(taskId).singleResult();

        List<NextFlowDto> nextFlowDtoList = new ArrayList<>();
        if (Objects.nonNull(task)) {
            // Step 2. 获取当前流程所有流程变量(网关节点时需要校验表达式)
            Map<String, Object> variables = taskService.getVariables(task.getId());
            List<UserTask> nextUserTask = FindNextNodeUtil.getNextUserTasks(repositoryService, task, variables);

            if (CollectionUtils.isNotEmpty(nextUserTask)) {
                for (UserTask userTask : nextUserTask) {
                    NextFlowDto nextFlowDto = new NextFlowDto();

                    nextFlowDto.processInstanceId = task.getProcessInstanceId();
                    nextFlowDto.nextFlowId = userTask.getId();
                    nextFlowDto.nextFlowName = userTask.getName();
                    nextFlowDto.sequenceFlowName = userTask.getAttributeValue(null, "sequenceFlowName");
                    nextFlowDto.isEnd = "1".equals(userTask.getAttributeValue(null, "isEnd"));
                    nextFlowDto.isReturn = "return".equals(userTask.getAttributeValue(null, "sequenceFlowDesc"));
                    nextFlowDto.nextUserType = String.join(",", userTask.getCandidateUsers().size() > 0 ? userTask.getCandidateUsers().stream().map(e -> e.substring(e.lastIndexOf("{") + 1, e.lastIndexOf("}"))).collect(Collectors.toList())
                            : userTask.getCandidateGroups().stream().map(e -> e.substring(e.lastIndexOf("{") + 1, e.lastIndexOf("}"))).collect(Collectors.toList()));

                    MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
                    // 会签节点
                    if (Objects.nonNull(multiInstance)) {
                        nextFlowDto.isMulti = true;
                        nextFlowDto.isMultiInstance = true;
                        nextFlowDto.multiInstanceUserType = multiInstance.getInputDataItem();
                    } else {
                        // 读取自定义节点属性 判断是否是否需要动态指定任务接收人员、组
                        String userType = userTask.getAttributeValue(ProcessConstants.NAMASPASE, ProcessConstants.PROCESS_CUSTOM_USER_TYPE);
                        nextFlowDto.isMulti = "assignee".equals(userType);
                    }

                    nextFlowDtoList.add(nextFlowDto);
                }
            }
        }
        return nextFlowDtoList;
    }

    /**
     * 完成任务
     *
     * @param taskCompleteDto 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void complete(TaskCompleteDto taskCompleteDto) throws Exception {
        if (!StringUtils.isNotBlank(taskCompleteDto.getTaskId()))
            throw new Exception("任务ID不能为空");
        if (!StringUtils.isNotBlank(taskCompleteDto.getProcessInstanceId()))
            throw new Exception("流程实例ID不能为空");
        if (!StringUtils.isNotBlank(taskCompleteDto.getAssignee()))
            throw new Exception("审核人不能为空");
        if (!StringUtils.isNotBlank(taskCompleteDto.getNextNodeId()))
            throw new Exception("下一节点ID不能为空");

        Task task = taskService.createTaskQuery().taskId(taskCompleteDto.getTaskId()).singleResult();
        if (Objects.isNull(task)) {
            throw new Exception("任务不存在");
        }

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process mainProcess = bpmnModel.getMainProcess();
        Collection<FlowElement> flowElements = mainProcess.getFlowElements();
        FlowElement nextNode = flowElements.stream().filter(it -> it.getId().equals(taskCompleteDto.getNextNodeId())).findFirst().orElse(null);

        if (nextNode == null)
            throw new Exception("下一节点不存在");
        if (!(nextNode instanceof EndEvent) && taskCompleteDto.getNextUserIds().size() == 0)
            throw new Exception("下一节点审核人不能为空");

        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            taskService.addComment(taskCompleteDto.getTaskId(), taskCompleteDto.getProcessInstanceId(), FlowComment.DELEGATE.getType(), taskCompleteDto.getComment());
            taskService.resolveTask(taskCompleteDto.getTaskId(), taskCompleteDto.getVariables());
        } else {
            taskService.addComment(taskCompleteDto.getTaskId(), taskCompleteDto.getProcessInstanceId(), FlowComment.NORMAL.getType(), taskCompleteDto.getComment());
            //taskService.setAssignee(taskCompleteDto.getTaskId(), taskCompleteDto.getAssignee());//更新任务审核人
            taskService.setVariableLocal(taskCompleteDto.getTaskId(), this.workUserParamName, taskCompleteDto.getAssignee());//更新任务审核人

            taskService.complete(taskCompleteDto.getTaskId(), taskCompleteDto.getVariables());

            //设置任务接收人
            List<Task> nextTaskList = taskService.createTaskQuery()
                    .active()
                    .includeProcessVariables().processInstanceId(task.getProcessInstanceId()).list().stream().filter(tk -> !tk.getTaskDefinitionKey().equals(task.getTaskDefinitionKey())).collect(Collectors.toList());
            if (nextTaskList.size() == 1)
                taskService.setAssignee(nextTaskList.get(0).getId(), String.join(",", taskCompleteDto.getNextUserIds()));
            else {
                for (int i = 0; i < nextTaskList.size(); i++) {
                    taskService.setAssignee(nextTaskList.get(i).getId(), taskCompleteDto.getNextUserIds().get(i));
                }
            }
        }
    }

    /**
     * 退回任务
     *
     * @param taskReturnDto 请求实体参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void taskReturn(TaskReturnDto taskReturnDto) throws Exception {
        if (!StringUtils.isNotBlank(taskReturnDto.getTaskId()))
            throw new Exception("任务ID不能为空");
        if (!StringUtils.isNotBlank(taskReturnDto.getProcessInstanceId()))
            throw new Exception("流程实例ID不能为空");
        if (!StringUtils.isNotBlank(taskReturnDto.getAssignee()))
            throw new Exception("审核人不能为空");
        if (!StringUtils.isNotBlank(taskReturnDto.getNextNodeId()))
            throw new Exception("下一节点ID不能为空");

        Task task = taskService.createTaskQuery().taskId(taskReturnDto.getTaskId()).singleResult();
        if (Objects.isNull(task)) {
            throw new Exception("任务不存在");
        }

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process mainProcess = bpmnModel.getMainProcess();
        Collection<FlowElement> flowElements = mainProcess.getFlowElements();
        FlowElement nextNode = flowElements.stream().filter(it -> it.getId().equals(taskReturnDto.getNextNodeId())).findFirst().orElse(null);

        if (nextNode == null)
            throw new Exception("下一节点不存在");
        if (!(nextNode instanceof EndEvent) && taskReturnDto.getNextUserIds().size() == 0)
            throw new Exception("下一节点审核人不能为空");

        /*if (taskService.createTaskQuery().taskId(taskReturnDto.getTaskId()).singleResult().isSuspended()) {
            throw new CustomException("任务处于挂起状态");
        }*/
        // 当前任务 task
        //Task task = taskService.createTaskQuery().taskId(taskReturnDto.getTaskId()).singleResult();
        // 获取流程定义信息
        //ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 获取所有节点信息
        Process process = repositoryService.getBpmnModel(processDefinition.getId()).getProcesses().get(0);
        // 获取全部节点列表，包含子节点
        Collection<FlowElement> allElements = FlowableUtils.getAllElements(process.getFlowElements(), null);
        // 获取当前任务节点元素
        FlowElement source = null;
        // 获取跳转的节点元素
        FlowElement target = null;
        if (allElements != null) {
            for (FlowElement flowElement : allElements) {
                // 当前任务节点元素
                if (flowElement.getId().equals(task.getTaskDefinitionKey())) {
                    source = flowElement;
                }
                // 跳转的节点元素
                if (flowElement.getId().equals(taskReturnDto.getNextNodeId())) {
                    target = flowElement;
                }
            }
        }

        // 从当前节点向前扫描
        // 如果存在路线上不存在目标节点，说明目标节点是在网关上或非同一路线上，不可跳转
        // 否则目标节点相对于当前节点，属于串行
        /*Boolean isSequential = FlowableUtils.iteratorCheckSequentialReferTarget(source, flowTaskVo.getTargetKey(), null, null);
        if (!isSequential) {
            throw new CustomException("当前节点相对于目标节点，不属于串行关系，无法回退");
        }*/


        // 获取所有正常进行的任务节点 Key，这些任务不能直接使用，需要找出其中需要撤回的任务
        List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        List<String> runTaskKeyList = new ArrayList<>();
        runTaskList.forEach(item -> runTaskKeyList.add(item.getTaskDefinitionKey()));
        // 需退回任务列表
        List<String> currentIds = new ArrayList<>();
        // 通过父级网关的出口连线，结合 runTaskList 比对，获取需要撤回的任务
        List<UserTask> currentUserTaskList = FlowableUtils.iteratorFindChildUserTasks(target, runTaskKeyList, null, null);
        currentUserTaskList.forEach(item -> currentIds.add(item.getId()));

        // 循环获取那些需要被撤回的节点的ID，用来设置驳回原因
        List<String> currentTaskIds = new ArrayList<>();
        currentIds.forEach(currentId -> runTaskList.forEach(runTask -> {
            if (currentId.equals(runTask.getTaskDefinitionKey())) {
                currentTaskIds.add(runTask.getId());
            }
        }));
        // 设置回退意见
        currentTaskIds.forEach(currentTaskId -> taskService.addComment(currentTaskId, task.getProcessInstanceId(), FlowComment.REBACK.getType(), taskReturnDto.getComment()));

        try {
            taskService.setVariable(taskReturnDto.getTaskId(), "skip", false);
            for (String key : taskReturnDto.getVariables().keySet()) {
                taskService.setVariable(taskReturnDto.getTaskId(), key, taskReturnDto.getVariables().get(key));
            }
            // 1 对 1 或 多 对 1 情况，currentIds 当前要跳转的节点列表(1或多)，targetKey 跳转到的节点(1)

            //更新当前步骤审核人
            //taskService.setAssignee(taskReturnDto.getTaskId(), taskReturnDto.getAssignee());

            taskService.setVariableLocal(taskReturnDto.getTaskId(), this.workUserParamName, taskReturnDto.getAssignee());//更新任务审核人

            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(currentIds, taskReturnDto.getNextNodeId()).changeState();

            //设置下一步骤待办人
            List<Task> nextTaskList = taskService.createTaskQuery()
                    .active()
                    .includeProcessVariables().processInstanceId(task.getProcessInstanceId()).list();
            if (nextTaskList.size() > 0)
                taskService.setAssignee(nextTaskList.get(0).getId(), String.join(",", taskReturnDto.getNextUserIds()));

        } catch (FlowableObjectNotFoundException e) {
            throw new CustomException("未找到流程实例，流程可能已发生变化");
        } catch (FlowableException e) {
            throw new CustomException("无法取消或开始活动");
        }
    }

    /**
     * 待办任务列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param userId   待办人id
     * @return
     */
    @Override
    public Page<FlowTodoDto> todoList(Integer pageNum, Integer pageSize, String userId) {
        Page<FlowTodoDto> page = new Page<>();
        TaskQuery taskQuery = taskService.createTaskQuery().active().includeProcessVariables()
                .taskAssigneeLike("%" + userId + "%")
                .orderByTaskCreateTime().desc();

        page.setTotal(taskQuery.count());
        List<Task> taskList = taskQuery.listPage(pageSize * (pageNum - 1), pageSize);
        List<FlowTodoDto> flowList = new ArrayList<>();
        for (Task task : taskList) {
            FlowTodoDto flowTask = new FlowTodoDto();
            // 当前流程信息
            flowTask.taskId = task.getId();
            flowTask.taskName = task.getName();
            flowTask.processInstanceId = task.getProcessInstanceId();
            flowTask.taskDefKey = task.getTaskDefinitionKey();
            flowTask.createTime = task.getCreateTime();
            flowTask.procDefId = task.getProcessDefinitionId();
            flowTask.executionId = task.getExecutionId();
            flowTask.owner = task.getOwner();
            flowTask.assignee = task.getAssignee();

            // 流程定义信息
            ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(task.getProcessDefinitionId())
                    .singleResult();
            flowTask.procDefName = pd.getName();
            flowTask.procDefVersion = pd.getVersion();

            // 流程发起人信息
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();

            flowTask.startUserId = historicProcessInstance.getStartUserId();

            flowTask.procVars = task.getProcessVariables();
            flowTask.taskVars = task.getTaskLocalVariables();

            flowList.add(flowTask);
        }

        page.setRecords(flowList);
        return page;
    }

    /**
     * 流程历史流转记录
     *
     * @param processInstanceId 流程实例Id
     * @return
     */
    @Override
    public List<FlowRecordDto> flowRecord(String processInstanceId) {
        List<FlowRecordDto> result = new ArrayList<>();
        if (StringUtils.isNotBlank(processInstanceId)) {
            List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceId().desc().list();
            List<HistoricTaskInstance> hisTaskinstList = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceId().desc().list();
            List<HistoricVariableInstance> hisVariableInstanceList = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByProcessInstanceId().desc().list();

            for (HistoricActivityInstance histIns : list) {
                if (StringUtils.isNotBlank(histIns.getTaskId())) {
                    FlowRecordDto flowTask = new FlowRecordDto();

                    flowTask.taskId = histIns.getTaskId();
                    flowTask.taskName = histIns.getActivityName();
                    flowTask.processInstanceId = histIns.getProcessInstanceId();
                    flowTask.createTime = histIns.getStartTime();
                    flowTask.finishTime = histIns.getEndTime();
                    flowTask.assignee = histIns.getAssignee();
                    HistoricTaskInstance task = hisTaskinstList.stream().filter(tk -> tk.getId().equals(histIns.getTaskId())).findFirst().orElse(null);
                    flowTask.procVars = task.getProcessVariables();
                    flowTask.taskVars = task.getTaskLocalVariables();

                    // 获取意见评论内容
                    List<Comment> commentList = taskService.getProcessInstanceComments(histIns.getProcessInstanceId());
                    commentList.forEach(comment -> {
                        if (histIns.getTaskId().equals(comment.getTaskId())) {
                            FlowCommentDto flowComment = FlowCommentDto.builder().type(comment.getType()).comment(comment.getFullMessage()).build();
                            if (flowComment != null) {
                                flowTask.setComment(flowComment.getComment());
                                flowTask.setCommentType(flowComment.getType());
                            }

                        }
                    });

                    //获取待办处理人
                    HistoricVariableInstance workUser = hisVariableInstanceList.stream().filter(it -> histIns.getTaskId().equals(it.getTaskId()) && this.workUserParamName.equals(it.getVariableName())).findFirst().orElse(null);
                    flowTask.workUserId = workUser != null ? workUser.getValue().toString() : "";

                    result.add(flowTask);
                }
            }

        }

        return result;
    }

}
