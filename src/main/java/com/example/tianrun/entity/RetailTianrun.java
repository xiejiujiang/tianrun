package com.example.tianrun.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;
import lombok.Data;

@Data
public class RetailTianrun extends BaseRowModel {

    @ExcelProperty(index = 0)
    private String plannedpickingdate;// 计划提货日期

    @ExcelProperty(index = 1)
    private String plansalecode;//发货计划单号

    @ExcelProperty(index = 2)
    private String partnerjx;//上游（供应商简写）

    @ExcelProperty(index = 3)
    private String customer;//客户

    @ExcelProperty(index = 4)
    private String getcustomer;//提货客户

    @ExcelProperty(index = 5)
    private String getdesc;//提货说明

    @ExcelProperty(index = 6)
    private String inventory;//物料(存货名称的简写)

    @ExcelProperty(index = 7)
    private String packing;//包装

    @ExcelProperty(index = 8)
    private String deliveryplace;// 交货地点

    @ExcelProperty(index = 9)
    private String contractcode;//合同编号

    @ExcelProperty(index = 10)
    private String taxprice;//含税单价

    @ExcelProperty(index = 11)
    private String drivername;//司机

    @ExcelProperty(index = 12)
    private String drivercdcard;//司机身份证

    @ExcelProperty(index = 13)
    private String drivermobile;//司机电话

    @ExcelProperty(index = 14)
    private String cardcode;//车牌号

    @ExcelProperty(index = 15)
    private String plansalenumbers;// 计划发货数量

    @ExcelProperty(index = 16)
    private String hebingnumber;// 合并行号

    @ExcelProperty(index = 17)
    private String createorderflag;// 生单标志

    @ExcelProperty(index = 18)
    private String rowmemo;// 行备注

    @ExcelProperty(index = 19)
    private String Tpartencode;//T+里面的供应商编码

    @ExcelProperty(index = 20)
    private String Tcustmorcode;//T+里面的客户编码

    @ExcelProperty(index = 21)
    private String Tinventorycode;//T+里面的存货编码

    @ExcelProperty(index = 22)
    private String sasourceVoucherDetailId;//T+的来源单据明细ID  用于 关联 销售订单（合同）

    @ExcelProperty(index = 23)
    private String pusourceVoucherDetailId;//T+的来源单据明细ID  用于 关联 采购订单（合同）

    @ExcelProperty(index = 24) //T+的税率吧
    private String taxnum;

    @ExcelProperty(index = 25) //T+的单位
    private String unitname;

    @ExcelProperty(index = 26) //蛋白查
    private String danbaicha;

    @ExcelProperty(index = 27) //销货单号
    private String sacode;

    @ExcelProperty(index = 28) //项目编号（实际上是根据油厂的名称查出来的）
    private String projectCode;

    @ExcelProperty(index = 29) //来源单据编号
    private String sourceVoucherCode;

    @ExcelProperty(index = 30) //结算客户编码
    private String settleCustomer;

    @ExcelProperty(index = 31) //T+的部门编码
    private String departmentCode;

    @ExcelProperty(index = 32) //T+的员工编码
    private String psersonCode;

    public String getPlannedpickingdate() {
        return plannedpickingdate;
    }

    public void setPlannedpickingdate(String plannedpickingdate) {
        this.plannedpickingdate = plannedpickingdate;
    }

    public String getPlansalecode() {
        return plansalecode;
    }

    public void setPlansalecode(String plansalecode) {
        this.plansalecode = plansalecode;
    }

    public String getPartnerjx() {
        return partnerjx;
    }

    public void setPartnerjx(String partnerjx) {
        this.partnerjx = partnerjx;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getGetcustomer() {
        return getcustomer;
    }

    public void setGetcustomer(String getcustomer) {
        this.getcustomer = getcustomer;
    }

    public String getGetdesc() {
        return getdesc;
    }

    public void setGetdesc(String getdesc) {
        this.getdesc = getdesc;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }

    public String getPacking() {
        return packing;
    }

    public void setPacking(String packing) {
        this.packing = packing;
    }

    public String getDeliveryplace() {
        return deliveryplace;
    }

    public void setDeliveryplace(String deliveryplace) {
        this.deliveryplace = deliveryplace;
    }

    public String getContractcode() {
        return contractcode;
    }

    public void setContractcode(String contractcode) {
        this.contractcode = contractcode;
    }

    public String getTaxprice() {
        return taxprice;
    }

    public void setTaxprice(String taxprice) {
        this.taxprice = taxprice;
    }

    public String getDrivername() {
        return drivername;
    }

    public void setDrivername(String drivername) {
        this.drivername = drivername;
    }

    public String getDrivercdcard() {
        return drivercdcard;
    }

    public void setDrivercdcard(String drivercdcard) {
        this.drivercdcard = drivercdcard;
    }

    public String getDrivermobile() {
        return drivermobile;
    }

    public void setDrivermobile(String drivermobile) {
        this.drivermobile = drivermobile;
    }

    public String getCardcode() {
        return cardcode;
    }

    public void setCardcode(String cardcode) {
        this.cardcode = cardcode;
    }

    public String getPlansalenumbers() {
        return plansalenumbers;
    }

    public void setPlansalenumbers(String plansalenumbers) {
        this.plansalenumbers = plansalenumbers;
    }

    public String getHebingnumber() {
        return hebingnumber;
    }

    public void setHebingnumber(String hebingnumber) {
        this.hebingnumber = hebingnumber;
    }

    public String getCreateorderflag() {
        return createorderflag;
    }

    public void setCreateorderflag(String createorderflag) {
        this.createorderflag = createorderflag;
    }

    public String getRowmemo() {
        return rowmemo;
    }

    public void setRowmemo(String rowmemo) {
        this.rowmemo = rowmemo;
    }

    public String getTpartencode() {
        return Tpartencode;
    }

    public void setTpartencode(String tpartencode) {
        Tpartencode = tpartencode;
    }

    public String getTcustmorcode() {
        return Tcustmorcode;
    }

    public void setTcustmorcode(String tcustmorcode) {
        Tcustmorcode = tcustmorcode;
    }

    public String getTinventorycode() {
        return Tinventorycode;
    }

    public void setTinventorycode(String tinventorycode) {
        Tinventorycode = tinventorycode;
    }

    public String getSasourceVoucherDetailId() {
        return sasourceVoucherDetailId;
    }

    public void setSasourceVoucherDetailId(String sasourceVoucherDetailId) {
        this.sasourceVoucherDetailId = sasourceVoucherDetailId;
    }

    public String getPusourceVoucherDetailId() {
        return pusourceVoucherDetailId;
    }

    public void setPusourceVoucherDetailId(String pusourceVoucherDetailId) {
        this.pusourceVoucherDetailId = pusourceVoucherDetailId;
    }

    public String getTaxnum() {
        return taxnum;
    }

    public void setTaxnum(String taxnum) {
        this.taxnum = taxnum;
    }

    public String getUnitname() {
        return unitname;
    }

    public void setUnitname(String unitname) {
        this.unitname = unitname;
    }

    public String getDanbaicha() {
        return danbaicha;
    }

    public void setDanbaicha(String danbaicha) {
        this.danbaicha = danbaicha;
    }

    public String getSacode() {
        return sacode;
    }

    public void setSacode(String sacode) {
        this.sacode = sacode;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getSourceVoucherCode() {
        return sourceVoucherCode;
    }

    public void setSourceVoucherCode(String sourceVoucherCode) {
        this.sourceVoucherCode = sourceVoucherCode;
    }

    public String getSettleCustomer() {
        return settleCustomer;
    }

    public void setSettleCustomer(String settleCustomer) {
        this.settleCustomer = settleCustomer;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getPsersonCode() {
        return psersonCode;
    }

    public void setPsersonCode(String psersonCode) {
        this.psersonCode = psersonCode;
    }

    @Override
    public String toString() {
        return "RetailTianrun{" +
                "plannedpickingdate='" + plannedpickingdate + '\'' +
                ", plansalecode='" + plansalecode + '\'' +
                ", partnerjx='" + partnerjx + '\'' +
                ", customer='" + customer + '\'' +
                ", getcustomer='" + getcustomer + '\'' +
                ", getdesc='" + getdesc + '\'' +
                ", inventory='" + inventory + '\'' +
                ", packing='" + packing + '\'' +
                ", deliveryplace='" + deliveryplace + '\'' +
                ", contractcode='" + contractcode + '\'' +
                ", taxprice='" + taxprice + '\'' +
                ", drivername='" + drivername + '\'' +
                ", drivercdcard='" + drivercdcard + '\'' +
                ", drivermobile='" + drivermobile + '\'' +
                ", cardcode='" + cardcode + '\'' +
                ", plansalenumbers='" + plansalenumbers + '\'' +
                ", hebingnumber='" + hebingnumber + '\'' +
                ", createorderflag='" + createorderflag + '\'' +
                ", rowmemo='" + rowmemo + '\'' +
                ", Tpartencode='" + Tpartencode + '\'' +
                ", Tcustmorcode='" + Tcustmorcode + '\'' +
                ", Tinventorycode='" + Tinventorycode + '\'' +
                ", sasourceVoucherDetailId='" + sasourceVoucherDetailId + '\'' +
                ", pusourceVoucherDetailId='" + pusourceVoucherDetailId + '\'' +
                ", taxnum='" + taxnum + '\'' +
                ", unitname='" + unitname + '\'' +
                ", danbaicha='" + danbaicha + '\'' +
                ", sacode='" + sacode + '\'' +
                ", projectCode='" + projectCode + '\'' +
                ", sourceVoucherCode='" + sourceVoucherCode + '\'' +
                ", settleCustomer='" + settleCustomer + '\'' +
                ", departmentCode='" + departmentCode + '\'' +
                ", psersonCode='" + psersonCode + '\'' +
                '}';
    }
}
