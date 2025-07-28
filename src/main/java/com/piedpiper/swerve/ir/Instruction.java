package com.piedpiper.swerve.ir;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.util.List;

@Data
@With
@AllArgsConstructor
@Builder
public class Instruction {
    private String label;
    private boolean global;
    private String result;
    List<Integer> indexes;
    private String operand1;
    private IROpcode operator;
    private String operand2;


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
