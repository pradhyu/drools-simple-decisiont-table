package com.example.drools.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Control fact used to manage rule execution flow.
 * Rules can insert this fact to signal that certain rule groups
 * should be suppressed or triggered.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleControl {
    private String controlType; // e.g., "STOP_PRIORITY_RULES", "ENABLE_SPECIAL_PROCESSING"
    private String reason; // Why this control was activated
}
