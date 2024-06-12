package org.relzx.pubsub.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessResult {
    SUCCESS("success"),
    FAILED("failed");
    private final String val;
}
