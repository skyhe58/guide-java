package com.example.basics.algorithm.sort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 排序算法正确性验证
 */
@DisplayName("排序算法测试")
class SortingTest {

    @Test
    @DisplayName("快速排序 — 普通数组")
    void quickSort_normalArray() {
        int[] arr = {38, 27, 43, 3, 9, 82, 10};
        SortingAlgorithms.quickSort(arr, 0, arr.length - 1);
        assertArrayEquals(new int[]{3, 9, 10, 27, 38, 43, 82}, arr);
    }

    @Test
    @DisplayName("快速排序 — 空数组")
    void quickSort_emptyArray() {
        int[] arr = {};
        SortingAlgorithms.quickSort(arr, 0, arr.length - 1);
        assertArrayEquals(new int[]{}, arr);
    }

    @Test
    @DisplayName("快速排序 — 单元素")
    void quickSort_singleElement() {
        int[] arr = {42};
        SortingAlgorithms.quickSort(arr, 0, 0);
        assertArrayEquals(new int[]{42}, arr);
    }

    @Test
    @DisplayName("快速排序 — 已排序数组")
    void quickSort_alreadySorted() {
        int[] arr = {1, 2, 3, 4, 5};
        SortingAlgorithms.quickSort(arr, 0, arr.length - 1);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, arr);
    }

    @Test
    @DisplayName("快速排序 — 逆序数组")
    void quickSort_reverseSorted() {
        int[] arr = {5, 4, 3, 2, 1};
        SortingAlgorithms.quickSort(arr, 0, arr.length - 1);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, arr);
    }

    @Test
    @DisplayName("快速排序 — 含重复元素")
    void quickSort_duplicates() {
        int[] arr = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3};
        SortingAlgorithms.quickSort(arr, 0, arr.length - 1);
        assertArrayEquals(new int[]{1, 1, 2, 3, 3, 4, 5, 5, 6, 9}, arr);
    }

    @Test
    @DisplayName("归并排序 — 普通数组")
    void mergeSort_normalArray() {
        int[] arr = {38, 27, 43, 3, 9, 82, 10};
        SortingAlgorithms.mergeSort(arr, 0, arr.length - 1);
        assertArrayEquals(new int[]{3, 9, 10, 27, 38, 43, 82}, arr);
    }

    @Test
    @DisplayName("归并排序 — 空数组")
    void mergeSort_emptyArray() {
        int[] arr = {};
        SortingAlgorithms.mergeSort(arr, 0, arr.length - 1);
        assertArrayEquals(new int[]{}, arr);
    }

    @Test
    @DisplayName("归并排序 — 含负数")
    void mergeSort_negativeNumbers() {
        int[] arr = {-3, 5, -1, 0, 8, -7};
        SortingAlgorithms.mergeSort(arr, 0, arr.length - 1);
        assertArrayEquals(new int[]{-7, -3, -1, 0, 5, 8}, arr);
    }

    @Test
    @DisplayName("堆排序 — 普通数组")
    void heapSort_normalArray() {
        int[] arr = {38, 27, 43, 3, 9, 82, 10};
        SortingAlgorithms.heapSort(arr);
        assertArrayEquals(new int[]{3, 9, 10, 27, 38, 43, 82}, arr);
    }

    @Test
    @DisplayName("堆排序 — 空数组")
    void heapSort_emptyArray() {
        int[] arr = {};
        SortingAlgorithms.heapSort(arr);
        assertArrayEquals(new int[]{}, arr);
    }

    @Test
    @DisplayName("堆排序 — 全部相同元素")
    void heapSort_allSame() {
        int[] arr = {5, 5, 5, 5};
        SortingAlgorithms.heapSort(arr);
        assertArrayEquals(new int[]{5, 5, 5, 5}, arr);
    }

    @Test
    @DisplayName("三种排序算法结果一致")
    void allSorts_sameResult() {
        int[] original = {64, 34, 25, 12, 22, 11, 90};
        int[] arr1 = Arrays.copyOf(original, original.length);
        int[] arr2 = Arrays.copyOf(original, original.length);
        int[] arr3 = Arrays.copyOf(original, original.length);

        SortingAlgorithms.quickSort(arr1, 0, arr1.length - 1);
        SortingAlgorithms.mergeSort(arr2, 0, arr2.length - 1);
        SortingAlgorithms.heapSort(arr3);

        assertArrayEquals(arr1, arr2, "快排和归并结果应一致");
        assertArrayEquals(arr2, arr3, "归并和堆排结果应一致");
    }
}
