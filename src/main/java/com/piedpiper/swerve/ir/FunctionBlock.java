package com.piedpiper.swerve.ir;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FunctionBlock {
    private String name;
    private List<String> parameters;
    private List<Instruction> instructions;
    private Map<String, Integer> labelledInstructions;

    public FunctionBlock(String name) {
        this.name = name;
        this.parameters = new ArrayList<>();
        this.instructions = new ArrayList<>();
        this.labelledInstructions = new HashMap<>();
    }

    public void addParam(String paramName) {
        this.parameters.add(paramName);
    }

    public void addInstruction(Instruction instruction) {
        if (instruction.getLabel() != null) {
            int index = instructions.size();
            labelledInstructions.put(instruction.getLabel(), index);
        }
        instructions.add(instruction);
    }

    public void addMultipleInstructions(List<Instruction> instructions) {
        this.instructions.addAll(instructions);
    }
}
