package com.ruoyi.flowable.domain.dto.flowpublish;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.flowable.domain.dto.FlowCommentDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.flowable.variable.api.history.HistoricVariableInstance;

import java.util.Date;
import java.util.List;

@Data
@ApiModel("工作流记录相关--返回参数")
public class FlowRecordDto {
    @ApiModelProperty("任务Id")
    public String taskId;

    @ApiModelProperty("流程实例d")
    public String processInstanceId;

    @ApiModelProperty("任务执行Id")
    public String executionId;

    @ApiModelProperty("任务名称")
    public String taskName;

    @ApiModelProperty("流程节点key")
    public String taskDefKey;

    @ApiModelProperty("流程Id")
    public String procDefId;

    @ApiModelProperty("流程key")
    public String procDefKey;

    @ApiModelProperty("流程定义名称")
    public String procDefName;

    @ApiModelProperty("流程定义内置使用版本")
    public int procDefVersion;

    @ApiModelProperty("流程变量信息")
    public Object procVars;

    @ApiModelProperty("任务变量信息")
    public Object taskVars;

    @ApiModelProperty("任务创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date createTime;

    @ApiModelProperty("任务完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date finishTime;

    @ApiModelProperty("流程发起人Id")
    public String startUserId;

    /*@ApiModelProperty("待办人Id")
    public String owner;*/

    @ApiModelProperty("任务待办人Id")
    public String assignee;

    @ApiModelProperty("任务处理人Id")
    public String workUserId;

    @ApiModelProperty("任务意见")
    private String comment;

    /**
     * 意见类别 0 正常意见  1 退回意见 2 驳回意见
     */
    @ApiModelProperty("任务意见类别，0 正常意见  1 退回意见 2 驳回意见")
    private String commentType;

    /*@ApiModelProperty("流程变量信息")
    private List<HistoricVariableInstance> variableList;*/
}
