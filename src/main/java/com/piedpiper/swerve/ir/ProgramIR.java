package com.piedpiper.swerve.ir;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Data
public class ProgramIR {
    List<FunctionIR> functions = new ArrayList<>();

    public void addFunction(FunctionIR function) {
        functions.add(function);
    }
}
