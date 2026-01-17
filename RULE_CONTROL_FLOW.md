# Rule Control Flow Pattern

This document explains the **Rule Control Flow** pattern implemented in this Drools application, which demonstrates how to use facts inserted from decision tables to control rule execution order and implement "stop after first match" logic.

## Overview

The Rule Control Flow pattern allows rules to:
1. **Insert control facts** during rule execution
2. **Suppress other rules** by checking for the presence/absence of control facts
3. **Trigger conditional processing** only when specific control facts exist
4. **Implement priority-based execution** with automatic stopping after the first match

## Implementation

### 1. Control Fact Model

We created a `RuleControl` model class to represent control signals:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleControl {
    private String controlType;  // e.g., "ESCALATION_PROCESSED"
    private String reason;       // Why this control was activated
}
```

### 2. Priority Escalation Rules

The `PriorityEscalation` rule table demonstrates **"stop after first match"** logic:

```csv
"RuleTable PriorityEscalation"
"PRIORITY","CONDITION","CONDITION","CONDITION","ACTION","ACTION"
"","c:Claim","","a:Audit","",""
"salience $param","amount > $param","not RuleControl(controlType == ""ESCALATION_PROCESSED"")","eval($param)","insert(new RuleControl(""ESCALATION_PROCESSED"", ""$param""));","a.addAudit(""Priority: $param"");"
"Order","Amount Threshold","No Prior Escalation","Audit Binding","Insert Control Fact","Log Message"
"1000","50000","true","true","Claim > 50k - CRITICAL ESCALATION - Stopping lower priority rules","CRITICAL: Claim exceeds 50k threshold"
"500","25000","true","true","Claim > 25k - HIGH ESCALATION - Stopping lower priority rules","HIGH: Claim exceeds 25k threshold"
"100","10000","true","true","Claim > 10k - MEDIUM ESCALATION - Stopping lower priority rules","MEDIUM: Claim exceeds 10k threshold"
```

**How it works:**

1. **Salience (Priority)**: Rules are ordered by salience (1000 > 500 > 100), so CRITICAL fires first
2. **Condition Check**: Each rule checks `not RuleControl(controlType == "ESCALATION_PROCESSED")`
3. **Control Fact Insertion**: When a rule fires, it inserts a `RuleControl` fact
4. **Suppression**: Lower-priority rules won't fire because the control fact now exists

**Example Flow:**
- Claim amount = $60,000
- CRITICAL rule fires (salience 1000) → inserts RuleControl
- HIGH rule is blocked (control fact exists)
- MEDIUM rule is blocked (control fact exists)
- **Result**: Only CRITICAL fires!

### 3. Conditional Processing Rules

The `ConditionalProcessing` rule table demonstrates **triggered execution**:

```csv
"RuleTable ConditionalProcessing"
"PRIORITY","CONDITION","CONDITION","CONDITION","ACTION"
"","RuleControl","c:Claim","a:Audit",""
"salience $param","controlType == ""$param""","amount > $param","eval($param)","a.addAudit(""Conditional: $param"");"
"Order","Control Type Required","Additional Condition","Audit Binding","Log Message"
"50","ESCALATION_PROCESSED","1000","true","Special processing triggered because escalation occurred"
```

**How it works:**

1. **Requires Control Fact**: Rule only fires if `RuleControl(controlType == "ESCALATION_PROCESSED")` exists
2. **Additional Conditions**: Can add more conditions (e.g., claim amount > 1000)
3. **Conditional Execution**: This rule ONLY runs if an escalation rule fired first

## Generated DRL

The decision table generates the following DRL rules:

```drl
// CRITICAL Escalation (highest priority)
rule "PriorityEscalation_65"
    salience 1000
    when
        c:Claim(amount > 50000)
        not RuleControl(controlType == "ESCALATION_PROCESSED")
        a:Audit(eval(true))
    then
        insert(new RuleControl("ESCALATION_PROCESSED", "Claim > 50k - CRITICAL ESCALATION"));
        a.addAudit("Priority: CRITICAL: Claim exceeds 50k threshold");
end

// HIGH Escalation (medium priority)
rule "PriorityEscalation_66"
    salience 500
    when
        c:Claim(amount > 25000)
        not RuleControl(controlType == "ESCALATION_PROCESSED")
        a:Audit(eval(true))
    then
        insert(new RuleControl("ESCALATION_PROCESSED", "Claim > 25k - HIGH ESCALATION"));
        a.addAudit("Priority: HIGH: Claim exceeds 25k threshold");
end

// Conditional Processing (only if escalation occurred)
rule "ConditionalProcessing_74"
    salience 50
    when
        RuleControl(controlType == "ESCALATION_PROCESSED")
        c:Claim(amount > 1000)
        a:Audit(eval(true))
    then
        a.addAudit("Conditional: Special processing triggered because escalation occurred");
end
```

## Test Cases

### Test 1: Critical Escalation (Stop After First Match)

```java
@Test
void testPriorityEscalationCritical() {
    Person p = new Person("HighValuePatient", 40, "Male");
    Claim c = new Claim("C100", 60000.0, "Medical");
    
    Audit audit = rulesService.executeRules(new EvaluationRequest(p, List.of(c)));
    
    // ✅ CRITICAL fires
    assertTrue(audit.getAudits().contains("Priority: CRITICAL: Claim exceeds 50k threshold"));
    
    // ❌ HIGH and MEDIUM are suppressed
    assertTrue(audit.getAudits().stream()
        .noneMatch(msg -> msg.contains("HIGH: Claim exceeds 25k threshold")));
    
    // ✅ Conditional processing fires (because control fact exists)
    assertTrue(audit.getAudits().contains("Conditional: Special processing triggered"));
}
```

### Test 2: Medium Escalation

```java
@Test
void testPriorityEscalationMedium() {
    Person p = new Person("MediumValuePatient", 35, "Female");
    Claim c = new Claim("C101", 15000.0, "Dental");
    
    Audit audit = rulesService.executeRules(new EvaluationRequest(p, List.of(c)));
    
    // ✅ MEDIUM fires (amount > 10k but < 25k)
    assertTrue(audit.getAudits().contains("Priority: MEDIUM: Claim exceeds 10k threshold"));
    
    // ❌ CRITICAL and HIGH don't fire (amount too low)
    assertTrue(audit.getAudits().stream()
        .noneMatch(msg -> msg.contains("CRITICAL:") || msg.contains("HIGH:")));
}
```

### Test 3: No Escalation

```java
@Test
void testNoEscalationNoConditional() {
    Person p = new Person("LowValuePatient", 30, "Male");
    Claim c = new Claim("C102", 5000.0, "Vision");
    
    Audit audit = rulesService.executeRules(new EvaluationRequest(p, List.of(c)));
    
    // ❌ No escalation rules fire (amount too low)
    assertTrue(audit.getAudits().stream()
        .noneMatch(msg -> msg.contains("CRITICAL:") || msg.contains("HIGH:") || msg.contains("MEDIUM:")));
    
    // ❌ Conditional processing doesn't fire (no control fact)
    assertTrue(audit.getAudits().stream()
        .noneMatch(msg -> msg.contains("Special processing triggered")));
}
```

## Use Cases

This pattern is useful for:

1. **Claim Adjudication**: Escalate high-value claims to different approval levels
2. **Risk Assessment**: Stop processing after identifying critical risk factors
3. **Workflow Control**: Trigger special processing only when certain conditions are met
4. **Performance Optimization**: Avoid evaluating unnecessary rules after a match
5. **Business Logic**: Implement "first match wins" semantics

## Key Concepts

### Salience (Priority)
- Higher salience = higher priority
- Rules with higher salience fire first
- Use to control execution order

### Control Facts
- Facts inserted during rule execution
- Used to signal state changes
- Can trigger or suppress other rules

### Negative Patterns
- `not RuleControl(...)` checks for absence of a fact
- Prevents rule from firing if control fact exists
- Implements "stop after first match" logic

### Positive Patterns
- `RuleControl(controlType == "...")` checks for presence of a fact
- Only fires if control fact exists
- Implements "conditional execution" logic

## Best Practices

1. **Use Descriptive Control Types**: Make control fact types self-documenting
2. **Set Appropriate Salience**: Ensure rules fire in the correct order
3. **Document Reasons**: Store why a control fact was inserted
4. **Test Edge Cases**: Verify behavior at boundary conditions
5. **Clean Up Control Facts**: Consider retracting control facts if needed for subsequent evaluations

## Conclusion

The Rule Control Flow pattern provides a powerful way to manage complex rule execution logic directly from decision tables. By inserting and checking for control facts, you can implement sophisticated workflows without writing custom Java code.
