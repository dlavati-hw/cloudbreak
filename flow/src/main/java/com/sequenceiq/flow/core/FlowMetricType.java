package com.sequenceiq.flow.core;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum FlowMetricType implements Metric {
    FLOW_STEP("flowstep"),
    ACTIVE_FLOWS("activeflow");

    private final String metricName;

    FlowMetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
