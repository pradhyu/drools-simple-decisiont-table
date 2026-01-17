# Rule Control Flow - Quick Example

## What Was Added

This example demonstrates **rule control flow** in Drools decision tables, where rules can insert facts to control the execution of other rules.

## The Pattern

### Scenario: Claim Escalation with "Stop After First Match"

When processing insurance claims, we want to:
1. Categorize claims by severity (CRITICAL > HIGH > MEDIUM)
2. **Only trigger the highest matching severity level**
3. Enable special processing when any escalation occurs

### How It Works

#### Step 1: Define a Control Fact

```java
public class RuleControl {
    private String controlType;  // "ESCALATION_PROCESSED"
    private String reason;
}
```

#### Step 2: Create Priority Rules That Insert Control Facts

```csv
"RuleTable PriorityEscalation"
Rule 1: If claim > $50k AND no escalation processed → Insert control fact, log "CRITICAL"
Rule 2: If claim > $25k AND no escalation processed → Insert control fact, log "HIGH"  
Rule 3: If claim > $10k AND no escalation processed → Insert control fact, log "MEDIUM"
```

**Key**: Each rule checks `not RuleControl(controlType == "ESCALATION_PROCESSED")` before firing.

#### Step 3: Create Conditional Rules That Require Control Facts

```csv
"RuleTable ConditionalProcessing"
Rule: If RuleControl exists AND claim > $1k → Log "Special processing triggered"
```

## Example Execution

### Claim Amount: $60,000

```
1. CRITICAL rule fires (salience 1000)
   ✅ Condition: amount > 50000 ✓
   ✅ Condition: not RuleControl(...) ✓ (no control fact yet)
   → Inserts RuleControl("ESCALATION_PROCESSED", "...")
   → Logs "CRITICAL: Claim exceeds 50k threshold"

2. HIGH rule is evaluated (salience 500)
   ✅ Condition: amount > 25000 ✓
   ❌ Condition: not RuleControl(...) ✗ (control fact exists!)
   → Rule does NOT fire

3. MEDIUM rule is evaluated (salience 100)
   ✅ Condition: amount > 10000 ✓
   ❌ Condition: not RuleControl(...) ✗ (control fact exists!)
   → Rule does NOT fire

4. Conditional Processing rule fires (salience 50)
   ✅ Condition: RuleControl exists ✓
   ✅ Condition: amount > 1000 ✓
   → Logs "Special processing triggered because escalation occurred"
```

**Result**: Only CRITICAL and Conditional Processing fire!

### Claim Amount: $5,000

```
1. CRITICAL rule is evaluated
   ❌ Condition: amount > 50000 ✗
   → Rule does NOT fire

2. HIGH rule is evaluated
   ❌ Condition: amount > 25000 ✗
   → Rule does NOT fire

3. MEDIUM rule is evaluated
   ❌ Condition: amount > 10000 ✗
   → Rule does NOT fire

4. Conditional Processing rule is evaluated
   ❌ Condition: RuleControl exists ✗ (no control fact inserted)
   → Rule does NOT fire
```

**Result**: No escalation rules fire!

## Test Results

```bash
mvn test
```

✅ **13 tests passed**, including:
- `testPriorityEscalationCritical()` - Verifies only CRITICAL fires for $60k claim
- `testPriorityEscalationMedium()` - Verifies only MEDIUM fires for $15k claim
- `testNoEscalationNoConditional()` - Verifies no escalation for $5k claim

## Key Benefits

1. **Stop After First Match**: Only the highest priority rule fires
2. **Declarative Control**: No Java code needed, all in CSV
3. **Conditional Execution**: Trigger rules only when certain conditions are met
4. **Performance**: Avoid evaluating unnecessary rules
5. **Maintainable**: Business users can modify thresholds in CSV

## Files Changed

- ✅ `RuleControl.java` - New control fact model
- ✅ `clinical-rules.csv` - Added PriorityEscalation and ConditionalProcessing tables
- ✅ `DroolsApplicationTests.java` - Added 3 new test cases
- ✅ `RULE_CONTROL_FLOW.md` - Comprehensive documentation
- ✅ `README.md` - Updated feature list

## Build Status

```
[INFO] BUILD SUCCESS
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
```
