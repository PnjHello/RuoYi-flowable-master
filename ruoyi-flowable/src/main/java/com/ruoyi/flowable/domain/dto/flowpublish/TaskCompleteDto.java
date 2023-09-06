package com.ruoyi.flowable.domain.dto.flowpublish;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("工作流任务完成--请求参数")
public class TaskCompleteDto {
    @ApiModelProperty("任务Id，必填")
    private String taskId;

    @ApiModelProperty("任务意见")
    private String comment;

    @ApiModelProperty("流程实例Id，必填")
    private String processInstanceId;

    @ApiModelProperty("流程变量信息，json对象")
    private Map<String, Object> variables;

    @ApiModelProperty("审批人，必填")
    private String assignee;

    @ApiModelProperty("下一节点Id，必填")
    private String nextNodeId;

    @ApiModelProperty("下一步审核人，下一节点非结束时必填")
    private List<String> nextUserIds;
}
