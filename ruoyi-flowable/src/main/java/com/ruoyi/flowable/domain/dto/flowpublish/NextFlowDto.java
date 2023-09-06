package com.ruoyi.flowable.domain.dto.flowpublish;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("工作流下一节点--返回参数")
public class NextFlowDto {
    @ApiModelProperty("流程实例id")
    public String processInstanceId;

    @ApiModelProperty("下一节点id")
    public String nextFlowId;

    @ApiModelProperty("下一节点名称")
    public String nextFlowName;

    @ApiModelProperty("连接线名称")
    public String sequenceFlowName;

    @ApiModelProperty("是否结束节点")
    public boolean isEnd;

    @ApiModelProperty("是否退回节点")
    public boolean isReturn;

    @ApiModelProperty("审核人单选/多选")
    public boolean isMulti;

    @ApiModelProperty("是否会签")
    public boolean isMultiInstance;

    @ApiModelProperty("下一审核人类型")
    public String nextUserType;

    @ApiModelProperty("会签审核人类型")
    public String multiInstanceUserType;
}
