package com.piedpiper.swerve.ir;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Data
public class CFGBlock {
    String label;
    List<IRInstruction> instructions = new ArrayList<>();
    List<CFGBlock> parents = new ArrayList<>();
    List<CFGBlock> children = new ArrayList<>();

    public void addInstruction(IRInstruction instruction) {
        instructions.add(instruction);
    }
}
