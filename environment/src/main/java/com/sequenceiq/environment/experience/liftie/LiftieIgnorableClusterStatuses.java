package com.sequenceiq.environment.experience.liftie;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public enum LiftieIgnorableClusterStatuses {

    DELETED,
    DELETING;

    public boolean isNotEqualTo(String status) {
        return !isEqualTo(status);
    }

    public boolean isEqualTo(String status) {
        return name().equalsIgnoreCase(status);
    }

    public static boolean notContains(String status) {
        return !contains(status);
    }

    public static boolean contains(String status) {
        return !StringUtils.isEmpty(status) && Arrays.asList(LiftieIgnorableClusterStatuses.values())
                .stream()
                .map(s -> s.name().toLowerCase())
                .collect(toSet())
                .contains(status.toLowerCase());
    }

}
