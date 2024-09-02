package com.piedpiper.swerve.ir;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Data
public class IRInstruction {
    String operation;
    String destination;
    List<String> operands;
}
