package com.sequenceiq.datalake.flow.dr.event;

import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseDrStartBaseEvent extends SdxEvent  {
    private SdxOperation drStatus;

    public DatalakeDatabaseDrStartBaseEvent(String selector, Long sdxId, String userId,
            SdxOperationType operationType) {
        super(selector, sdxId, userId);
        drStatus = new SdxOperation(operationType, sdxId);
    }

    public DatalakeDatabaseDrStartBaseEvent(String selector, String userId,
                                            SdxOperation drStatus) {
        super(selector, drStatus.getSdxClusterId(), userId);
        this.drStatus = drStatus;
    }

    public SdxOperation getDrStatus() {
        return drStatus;
    }
}
