package com.ruoyi.flowable.domain.dto.flowpublish;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("工作流实例启动相关--请求参数")
public class FlowStartDto {
    @ApiModelProperty("流程定义Id，必填")
    private String procDefId;

    @ApiModelProperty("流程发起人Id，必填")
    private String userId;

    @ApiModelProperty("下一步处理人Id")
    private List<String> nextUserIds;

    @ApiModelProperty("流程变量信息，json对象")
    private Map<String, Object> variables;
}
