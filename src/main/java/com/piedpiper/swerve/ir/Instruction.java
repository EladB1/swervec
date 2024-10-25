package com.piedpiper.swerve.ir;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;

import java.util.List;

@Data
@With
@AllArgsConstructor
public class Instruction {
    private String label = null;
    private boolean global = false;
    private String result = null;
    List<Integer> indexes = null;
    private String operand1 = null;
    private IROpcode operator = null;
    private String operand2 = null;

    public Instruction(String result, List<Integer> indexes, String operand2) {
        this.result = result;
        this.indexes = indexes;
        this.operand2 = operand2;
    }

    public Instruction(String result, List<Integer> indexes, IROpcode operator, String operand2) {
        this.result = result;
        this.indexes = indexes;
        this.operator = operator;
        this.operand2 = operand2;
    }

    public Instruction(String result, List<Integer> indexes, String operand1, IROpcode operator, String operand2) {
        this.result = result;
        this.indexes = indexes;
        this.operand1 = operand1;
        this.operator = operator;
        this.operand2 = operand2;
    }

    public Instruction(IROpcode operator, String operand2) {
        this.operator = operator;
        this.operand2 = operand2;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(global ? "global " : "");
        if (!global && label != null) {
            output = new StringBuilder(label).append(": ");
        }
        if (result != null) {
            output.append(result);
            if (indexes != null) {
                for (int index : indexes) {
                    output.append(String.format("[%d]", index));
                }
            }
            output.append(" = ");
        }
        if (operand1 != null)
            output.append(operand1).append(" ");
        if (operator != null)
            output.append(operator).append(" ");
        if (operand2 != null)
            output.append(operand2);
        return output.toString();
    }
}
