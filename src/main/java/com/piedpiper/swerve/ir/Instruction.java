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
    private String operand1;
    private IROpcode operator = null;
    private String operand2 = null;

    public Instruction(String operand1) {
        this.operand1 = operand1;
    }

    public Instruction(String label, IROpcode operator) {
        this.label = label;
        this.operator = operator;
    }

    public Instruction(String result, String operand1) {
        this.result = result;
        this.operand1 = operand1;
    }

    public Instruction(String label, String result, String operand1) {
        this.label = label;
        this.result = result;
        this.operand1 = operand1;
    }

    public Instruction(IROpcode operator, String operand1) {
        this.operator = operator;
        this.operand1 = operand1;
    }

    public Instruction(IROpcode operator, String operand1, String operand2) {
        this.operator = operator;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    public Instruction(String result, IROpcode operator, String operand1) {
        this.result = result;
        this.operator = operator;
        this.operand1 = operand1;
    }

    public Instruction(String label, String result, IROpcode operator, String operand1) {
        this.label = label;
        this.result = result;
        this.operator = operator;
        this.operand1 = operand1;
    }

    public Instruction(String result, IROpcode operator, String operand1, String operand2) {
        this.result = result;
        this.operator = operator;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    public Instruction(String label, String result, IROpcode operator, String operand1, String operand2) {
        this.label = label;
        this.result = result;
        this.operator = operator;
        this.operand1 = operand1;
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
            output.append(" = ");
        }
        if (operand1 != null) {
            if (operator != null && operand2 == null) {
                output.append(operator).append(" ").append(operand1);
                return output.toString();
            }
            output.append(operand1);
        }

        if (operator != null)
            output.append(" ").append(operator).append(" ");
        if (operand2 != null)
            output.append(operand2);
        return output.toString();
    }
}
