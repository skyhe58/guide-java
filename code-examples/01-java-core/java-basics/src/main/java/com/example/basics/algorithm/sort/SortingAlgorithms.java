package com.example.basics.algorithm.sort;

import java.util.Arrays;

/**
 * 排序算法实现
 * 包含：快速排序、归并排序、堆排序
 *
 * 复杂度对比：
 * | 算法     | 平均时间     | 最坏时间     | 空间     | 稳定性 |
 * |---------|-------------|-------------|---------|--------|
 * | 快速排序 | O(n log n)  | O(n²)       | O(log n)| 不稳定 |
 * | 归并排序 | O(n log n)  | O(n log n)  | O(n)    | 稳定   |
 * | 堆排序   | O(n log n)  | O(n log n)  | O(1)    | 不稳定 |
 */
public class SortingAlgorithms {

    // ==================== 快速排序 ====================

    /**
     * 快速排序
     * 平均时间: O(n log n)，最坏: O(n²)
     * 空间: O(log n) 递归栈
     * 不稳定排序
     */
    public static void quickSort(int[] arr, int left, int right) {
        if (left >= right) return;
        int pivotIndex = partition(arr, left, right);
        quickSort(arr, left, pivotIndex - 1);
        quickSort(arr, pivotIndex + 1, right);
    }

    /**
     * 分区操作：选择 pivot，将小于 pivot 的放左边，大于的放右边
     * 使用随机选择 pivot 避免最坏情况
     */
    private static int partition(int[] arr, int left, int right) {
        // 随机选择 pivot
        int randomIdx = left + (int) (Math.random() * (right - left + 1));
        swap(arr, randomIdx, right);

        int pivot = arr[right];
        int i = left; // i 指向下一个小于 pivot 的位置
        for (int j = left; j < right; j++) {
            if (arr[j] < pivot) {
                swap(arr, i++, j);
            }
        }
        swap(arr, i, right);
        return i;
    }

    // ==================== 归并排序 ====================

    /**
     * 归并排序
     * 时间: O(n log n)（始终稳定）
     * 空间: O(n)
     * 稳定排序
     */
    public static void mergeSort(int[] arr, int left, int right) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSort(arr, left, mid);
        mergeSort(arr, mid + 1, right);
        merge(arr, left, mid, right);
    }

    /**
     * 合并两个有序子数组
     * arr[left..mid] 和 arr[mid+1..right]
     */
    private static void merge(int[] arr, int left, int mid, int right) {
        int[] temp = new int[right - left + 1];
        int i = left, j = mid + 1, k = 0;
        while (i <= mid && j <= right) {
            if (arr[i] <= arr[j]) { // <= 保证稳定性
                temp[k++] = arr[i++];
            } else {
                temp[k++] = arr[j++];
            }
        }
        while (i <= mid) temp[k++] = arr[i++];
        while (j <= right) temp[k++] = arr[j++];
        System.arraycopy(temp, 0, arr, left, temp.length);
    }

    // ==================== 堆排序 ====================

    /**
     * 堆排序
     * 时间: O(n log n)
     * 空间: O(1)
     * 不稳定排序
     */
    public static void heapSort(int[] arr) {
        int n = arr.length;
        // 1. 建堆：从最后一个非叶子节点开始下沉
        for (int i = n / 2 - 1; i >= 0; i--) {
            siftDown(arr, i, n);
        }
        // 2. 排序：每次将堆顶（最大值）与末尾交换，然后下沉调整
        for (int i = n - 1; i > 0; i--) {
            swap(arr, 0, i);     // 堆顶（最大值）放到末尾
            siftDown(arr, 0, i); // 缩小堆范围，下沉调整
        }
    }

    /**
     * 下沉操作：维护最大堆性质
     * @param arr  数组
     * @param i    当前节点下标
     * @param n    堆的大小
     */
    private static void siftDown(int[] arr, int i, int n) {
        while (2 * i + 1 < n) {
            int child = 2 * i + 1; // 左子节点
            // 选择较大的子节点
            if (child + 1 < n && arr[child + 1] > arr[child]) {
                child++;
            }
            if (arr[i] >= arr[child]) break; // 已满足堆性质
            swap(arr, i, child);
            i = child;
        }
    }

    // ==================== 工具方法 ====================

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    // ==================== 演示 ====================

    public static void main(String[] args) {
        int[] original = {38, 27, 43, 3, 9, 82, 10};

        // 1. 快速排序
        System.out.println("=== 快速排序 ===");
        int[] arr1 = Arrays.copyOf(original, original.length);
        System.out.println("排序前: " + Arrays.toString(arr1));
        quickSort(arr1, 0, arr1.length - 1);
        System.out.println("排序后: " + Arrays.toString(arr1));

        // 2. 归并排序
        System.out.println("\n=== 归并排序 ===");
        int[] arr2 = Arrays.copyOf(original, original.length);
        System.out.println("排序前: " + Arrays.toString(arr2));
        mergeSort(arr2, 0, arr2.length - 1);
        System.out.println("排序后: " + Arrays.toString(arr2));

        // 3. 堆排序
        System.out.println("\n=== 堆排序 ===");
        int[] arr3 = Arrays.copyOf(original, original.length);
        System.out.println("排序前: " + Arrays.toString(arr3));
        heapSort(arr3);
        System.out.println("排序后: " + Arrays.toString(arr3));

        // 4. 边界测试
        System.out.println("\n=== 边界测试 ===");
        int[] empty = {};
        heapSort(empty);
        System.out.println("空数组: " + Arrays.toString(empty));

        int[] single = {1};
        quickSort(single, 0, 0);
        System.out.println("单元素: " + Arrays.toString(single));

        int[] sorted = {1, 2, 3, 4, 5};
        mergeSort(sorted, 0, sorted.length - 1);
        System.out.println("已排序: " + Arrays.toString(sorted));
    }
}
