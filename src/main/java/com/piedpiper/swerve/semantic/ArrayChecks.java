package com.piedpiper.swerve.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.piedpiper.swerve.error.ArrayBoundsError;
import com.piedpiper.swerve.parser.AbstractSyntaxTree;

public class ArrayChecks {
    private static boolean isArrayLit(AbstractSyntaxTree arrayNode) {
        return arrayNode.getLabel().equals("ARRAY-LIT");
    }

    public static int getMaxDepth(AbstractSyntaxTree arrayNode) {
        return getMaxDepth(arrayNode, 1);
    }

    private static int getMaxDepth(AbstractSyntaxTree arrayNode, int depth) {
        if (!arrayNode.hasChildren() || !isArrayLit(arrayNode.getChildren().get(0)))
            return depth;
        int max_depth = 0;
        for (AbstractSyntaxTree element : arrayNode.getChildren()) {
            max_depth = Math.max(max_depth, getMaxDepth(element, depth+1));
        }
        return max_depth;
    }

    public static List<List<Integer>> getAllArraySizes(AbstractSyntaxTree arrayNode) {
        return getAllArraySizes(arrayNode, new ArrayList<>(), 0);
    }

    private static List<List<Integer>> getAllArraySizes(AbstractSyntaxTree arrayNode, List<List<Integer>> sizes, int index) {
        int depth = getMaxDepth(arrayNode);
        List<Integer> topLevel = List.of(arrayNode.countChildren());
        if (depth == 1)
            return List.of(topLevel);
        if (sizes.isEmpty()) {
            for (int i = 0; i < depth; i++) {
                sizes.add(new ArrayList<>());
            }
        }
        sizes.get(index).addAll(topLevel);
        List<List<Integer>> subSizes;
        for(AbstractSyntaxTree subArray : arrayNode.getChildren()) {
            subSizes = getAllArraySizes(subArray, sizes, index+1);
            if (subSizes != null)
                sizes.get(index+1).addAll(subSizes.get(0));
        }
        if (index == 0)
            return sizes;
        else
            return null;
    }

    /**
     * Given an array, return the maximum length at each array depth
     * Used for bounds checking (single and multidimensional) arrays
     * @param array
     * @return list of array sizes
     */
    public static List<Integer> estimateArraySizes(AbstractSyntaxTree array) {
        List<List<Integer>> arraySizes = getAllArraySizes(array);
        if (arraySizes.size() == 1)
            return arraySizes.get(0);
        List<Integer> maxes = new ArrayList<>();
        for (List<Integer> subList : arraySizes) {
            maxes.add(Collections.max(subList));
        }
        return maxes;
    }

    public static void checkArraySizeMatchesExpectation(AbstractSyntaxTree array, List<Integer> expectedSizes) {
        List<Integer> actualSizes = estimateArraySizes(array);
        if (actualSizes.size() > expectedSizes.size()) // less than or equal to is valid
            throw new ArrayBoundsError("Array value does not match declaration type");
        for (int i = 0; i < actualSizes.size(); i++) {
            if (actualSizes.get(i) > expectedSizes.get(i))
                throw new ArrayBoundsError("Array value size exceeds declared size");
        }
    }
}
