package com.ruoyi.flowable.domain.dto;

import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 动态人员、组
 * @author Tony
 * @date 2021/4/17 22:59
 */
@Data
public class FlowNextDto implements Serializable {

    private String type;
    private String dataType;
    private String vars;
    private String multiinstanceVars;
    private List<SysUser>nextUserList;
    private String nextFlowID;
    private String nextFlowName;
    private String sequenceFlowName;
    private boolean end;
    private boolean isReturn;

}
