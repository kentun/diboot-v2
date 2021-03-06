package com.diboot.file.example.test;

import com.diboot.core.util.JSON;
import com.diboot.core.vo.Status;
import com.diboot.file.example.custom.DepartmentExcelModel;
import com.diboot.file.example.custom.listener.DepartmentImportListener;
import com.diboot.file.util.ExcelHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * 固定表头excel读测试
 * @author mazc@dibo.ltd
 * @version v2.0
 * @date 2020/02/19
 */
public class FixedHeadExcelReadTest extends ExcelWriteTest {

    @Test
    public void testValidate(){
        DepartmentExcelModel department = new DepartmentExcelModel();
        department.setOrgName("dd");
        department.setParentId(0L);
        department.setMemCount(1);
        department.setUserStatus("S");
        //String msg = V.validateBean(department);
        //Assert.assertTrue(msg != null);
    }

    @Test
    public void testNormalDataRead(){
        try{
            prepareNormalDataExcel();
            // 读且保存
            DepartmentImportListener listener = new DepartmentImportListener();
            boolean success = ExcelHelper.previewReadExcel(getTempFilePath(), listener);
            Assert.assertTrue(success);
            System.out.println(JSON.stringify(listener.getFieldHeadMap()));
            System.out.println(JSON.stringify(listener.getDataList()));
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            Assert.fail();
        }
        finally {
            deleteTempFile();
        }
    }

    @Test
    public void testErrorDataRead(){
        try{
            prepareErrorDataExcel();
            // 预览读
            //ExcelHelper.previewRead(getTempFilePath(), new DepartmentImportListener());
            // 读且保存
            ExcelHelper.previewReadExcel(getTempFilePath(), new DepartmentImportListener());
        }
        catch (Exception e){
            e.printStackTrace();
            Assert.assertTrue(e.getMessage().contains(Status.FAIL_VALIDATION.label()));
        }
        finally {
            deleteTempFile();
        }
    }


}
