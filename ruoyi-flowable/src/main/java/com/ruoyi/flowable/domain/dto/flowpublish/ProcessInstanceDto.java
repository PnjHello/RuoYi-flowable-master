package com.ruoyi.flowable.domain.dto.flowpublish;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("工作流实例相关--返回参数")
public class ProcessInstanceDto {
    @ApiModelProperty("流程实例d")
    public String processInstanceId;
}
