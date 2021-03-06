package com.diboot.file.example.test;


import com.diboot.file.example.ApplicationTest;
import com.diboot.file.util.ExcelHelper;
import com.diboot.file.util.FileHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * excel写测试
 * @author mazc@dibo.ltd
 * @version v2.0
 * @date 2020/02/19
 */
public class ExcelWriteTest extends ApplicationTest {

    /**
     * 构建正常的数据
     * @return
     */
    public List<List<String>> buildNormalData(){
        List<List<String>> dataList = new ArrayList<>();
        dataList.add(Arrays.asList("父ID", "单位", "名称", "数量", "状态"));

        dataList.add(Arrays.asList("0", "dibo", "研发中心", "10", "在职"));
        dataList.add(Arrays.asList("0", "dibo", "营销中心", "10", "在职"));
        dataList.add(Arrays.asList("0", "dibo", "财务部", "10", "离职"));
        return dataList;
    }

    /**
     * 构建正常的数据
     * @return
     */
    public List<List<String>> buildErrorData(){
        List<List<String>> dataList = new ArrayList<>();
        dataList.add(Arrays.asList("父ID", "单位", "名称", "数量", "状态"));
        dataList.add(Arrays.asList("0", "dibo", "这个值超长了这个值超长了", "10", "正常"));
        dataList.add(Arrays.asList("0", "dibo", "营销中心", "10", "正常"));
        dataList.add(Arrays.asList("0", "other", "财务部", "10", "正常"));
        return dataList;
    }

    /**
     * 构建写的正常的数据
     * @return
     */
    public List<List<String>> buildWriteData(){
        List<List<String>> dataList = new ArrayList<>();
        dataList.add(Arrays.asList("父ID", "单位", "名称", "数量", "状态"));

        dataList.add(Arrays.asList("0", "dibo", "研发中心", "10", "A"));
        dataList.add(Arrays.asList("0", "dibo", "营销中心", "10", "A"));
        dataList.add(Arrays.asList("0", "dibo", "财务部", "10", "I"));
        return dataList;
    }

    /**
     * excel存储路径
     * @return
     */
    public String getTempFilePath(){
        return FileHelper.getSystemTempDir() + "temp.xlsx";
    }

    /**
     * 准备正常数据文件
     */
    public void prepareNormalDataExcel() throws Exception{
        ExcelHelper.writeDynamicData(getTempFilePath(), "部门列表", buildNormalData());
    }

    /**
     * 准备正常数据文件
     */
    public void prepareErrorDataExcel() throws Exception{
        ExcelHelper.writeDynamicData(getTempFilePath(), "部门列表", buildErrorData());
    }

    @Test
    public void testWrite() throws Exception{
        boolean success = ExcelHelper.writeDynamicData(getTempFilePath(), "部门列表", buildWriteData());
        Assert.assertTrue(success);
        System.out.println(getTempFilePath());
        deleteTempFile();
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(){
        // 删除临时文件
        FileHelper.deleteFile(getTempFilePath());
    }
}
