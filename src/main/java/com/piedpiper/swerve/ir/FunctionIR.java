package com.piedpiper.swerve.ir;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Data
public class FunctionIR {
    String name;
    List<CFGBlock> blocks = new ArrayList<>();

    public void addBlock(CFGBlock CFGBlock) {
        blocks.add(CFGBlock);
    }
}
