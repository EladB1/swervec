package com.piedpiper.swerve.ir;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Data
public class Block {
    String label;
    List<IRInstruction> instructions = new ArrayList<>();
    List<Block> parents = new ArrayList<>();
    List<Block> children = new ArrayList<>();

    public void addInstruction(IRInstruction instruction) {
        instructions.add(instruction);
    }
}
