package com.example.tianrun.utils;

import com.alibaba.fastjson.JSONObject;
import com.example.tianrun.entity.RetailTianrun;

import java.text.SimpleDateFormat;
import java.util.*;

public class ListToJson {

    public static List<String> getPuJsonByList(List<RetailTianrun> list){
        List<String> lsresut = new ArrayList<String>();
        for(RetailTianrun retailTianrun : list){
            Map<String,Object> dto = new HashMap<String,Object>();
            Map<String,Object> sa = new HashMap<String,Object>();
            Map<String,Object> Department = new HashMap<String,Object>();
            Department.put("Code","01");//部门编码
            sa.put("Department",Department);
            Map<String,Object> Clerk = new HashMap<String,Object>();
            Clerk.put("Code","0104");//业务员编码
            sa.put("Clerk",Clerk);
            sa.put("VoucherDate",retailTianrun.getPlannedpickingdate());//单据日期
            sa.put("ExternalCode",Md5.md5("XJJ"+Math.random()));//外部订单号，不可以重复（MD5，建议记录）
            Map<String,Object> Partner = new HashMap<String,Object>();
            Partner.put("Code",retailTianrun.getTpartencode());//供应商编码
            sa.put("Partner",Partner);
            Map<String,Object> BusinessType = new HashMap<String,Object>();
            BusinessType.put("Code","01");//业务类型编码，01--普通采购,02--采购退货
            sa.put("BusinessType",BusinessType);
            /*Map<String,Object> InvoiceType = new HashMap<String,Object>();
            InvoiceType.put("Code","01");//票据类型，枚举类型；00--普通发票，01--专用发票，02–收据；为空时，默认按收据处理
            sa.put("InvoiceType",InvoiceType);*/
            /*Map<String,Object> Warehouse = new HashMap<String,Object>();
            Warehouse.put("Code","0101010101");//表头上的 仓库编码
            sa.put("Warehouse",Warehouse);*/
            /*Map<String,Object> PayType = new HashMap<String,Object>();
            PayType.put("Code","05");//付款方式 : 00 限期付款 01 全额订金 02 全额现结 03 月结 05 其它
            sa.put("PayType",PayType);*/
            /*Map<String,Object> RdStyle = new HashMap<String,Object>();
            RdStyle.put("Code","201");//出库类别，RdStyleDTO对象，默认为“线上销售”类别； 具体值 我是查的数据库。
            sa.put("RdStyle",RdStyle);*/

            //进货单表头上的 采购订单号  PurchaseOrderCode
            Map<String,Object> PurchaseOrder = new HashMap<String,Object>();
            PurchaseOrder.put("Code",retailTianrun.getContractcode());//采购订单号(合同号)
            sa.put("PurchaseOrder",PurchaseOrder);

            sa.put("Memo",retailTianrun.getGetdesc());//备注

            //表头的自定义项
            List<String> biaotouilistkey = new ArrayList<String>();
            biaotouilistkey.add("priuserdefnvc1");//对应的销货单 单号
            List<String> biaitoulistvalue = new ArrayList<String>();
            biaitoulistvalue.add(retailTianrun.getSacode());//对应的销货单 单号
            sa.put("DynamicPropertyKeys",biaotouilistkey);
            sa.put("DynamicPropertyValues",biaitoulistvalue);

            //------------------------------------------ 商品明细 只有一行 ---------------------------------------------//
            List<Map<String,Object>> DeliveryDetailsList = new ArrayList<Map<String,Object>>();
            Map<String,Object> DetailM1 = new HashMap<String,Object>();
            Map<String,Object> DetailM1Inventory = new HashMap<String,Object>();
            DetailM1Inventory.put("code",retailTianrun.getTinventorycode());//明细1 的 存货编码
            DetailM1.put("Inventory",DetailM1Inventory);
            Map<String,Object> DetailM1Unit = new HashMap<String,Object>();
            DetailM1Unit.put("Name",retailTianrun.getUnitname());//明细1 的 存货计量单位
            DetailM1.put("Unit",DetailM1Unit);

            //油厂——》就是项目
            Map<String,Object> DetailM1Project = new HashMap<String,Object>();
            DetailM1Project.put("Code",retailTianrun.getProjectCode());// 是code ，不是 name
            DetailM1.put("Project",DetailM1Project);

            //DetailM1.put("Batch","？？？？？？？？？？？？？？？？？？？");//批号
            DetailM1.put("Quantity",retailTianrun.getPlansalenumbers());//明细1 的 数量
            //DetailM1.put("TaxRate",retailTianrun.getTaxnum());//明细1 的 税率

            //当票据类型为收据、专票且供应商报价不含税时（价外税），应输入金额OrigDiscountAmount或单价OrigDiscountPrice//明细1 的 不含税单价( 含税单价 / 1+税率)
            // 不含税单价
            System.out.println( retailTianrun.getTaxprice() + ", " + Float.valueOf(retailTianrun.getTaxnum()) );
            DetailM1.put("OrigDiscountPrice",Float.valueOf(retailTianrun.getTaxprice())/(1+(Float.valueOf(retailTianrun.getTaxnum())/100)));
            //DetailM1.put("OrigTaxPrice",retailTianrun.getTaxprice());//明细1 的 含税单价

            DetailM1.put("idsourcevouchertype","27");//明细1 的 来源单据类型ID
            DetailM1.put("sourceVoucherCode",retailTianrun.getContractcode());//明细1 的 来源单据单据编号 (合同号)
            DetailM1.put("sourceVoucherDetailId",retailTianrun.getPusourceVoucherDetailId());//明细1 的 来源单据单据对应的明细行ID

            // 自定义字段
            List<String> mingxilistkey = new ArrayList<String>();
            mingxilistkey.add("pubuserdefdecm7");//委托数量 ！
            mingxilistkey.add("pubuserdefdecm4");//蛋白差
            mingxilistkey.add("pubuserdefnvc1");//司机姓名 字符公用自定义项1
            mingxilistkey.add("pubuserdefnvc2");//司机车牌号 符公用自定义项2
            mingxilistkey.add("pubuserdefnvc5");//司机手机号 扩展公用自定义项5
            mingxilistkey.add("pubuserdefnvc6");//司机身份证号 扩展公用自定义项5
            mingxilistkey.add("priuserdefnvc1");// 行备注

            List<String> mingxilistvalue = new ArrayList<String>();
            mingxilistvalue.add(retailTianrun.getPlansalenumbers());//委托数量
            mingxilistvalue.add(retailTianrun.getDanbaicha());//蛋白差
            mingxilistvalue.add(retailTianrun.getDrivername());//司机姓名
            mingxilistvalue.add(retailTianrun.getCardcode());//司机车牌号
            mingxilistvalue.add(retailTianrun.getDrivermobile());//司机手机号
            mingxilistvalue.add(retailTianrun.getDrivercdcard());//司机身份证号
            mingxilistvalue.add(retailTianrun.getRowmemo());//行备注

            DetailM1.put("DynamicPropertyKeys",mingxilistkey);
            DetailM1.put("DynamicPropertyValues",mingxilistvalue);

            DeliveryDetailsList.add(DetailM1);
            //------------------------------------------ 商品明细 只有一行 ---------------------------------------------//

            sa.put("VoucherDetails",DeliveryDetailsList);
            dto.put("dto",sa);
            String js = JSONObject.toJSONString(dto);
            lsresut.add(js);
        }
        return lsresut;
    }


    public static List<String> getSaJsonByList(List<RetailTianrun> list){
        List<String> lsresut = new ArrayList<String>();
        for(RetailTianrun retailTianrun : list){
            Map<String,Object> dto = new HashMap<String,Object>();
            Map<String,Object> sa = new HashMap<String,Object>();

            sa.put("Code",retailTianrun.getSacode()); //这一笔 销货单的 单号！！

            Map<String,Object> Department = new HashMap<String,Object>();
            Department.put("Code","01");//部门编码
            sa.put("Department",Department);
            Map<String,Object> Clerk = new HashMap<String,Object>();
            Clerk.put("Code","0104");//业务员编码
            sa.put("Clerk",Clerk);
            //Map<String,Object> ReceiveType = new HashMap<String,Object>();
            //ReceiveType.put("Code","05");//收款方式 00--限期收款，01--全额订金，02--全额现结，03--月结，04--分期收款，05--其它
            //sa.put("ReceiveType",ReceiveType);
            sa.put("VoucherDate",retailTianrun.getPlannedpickingdate());//单据日期
            sa.put("ExternalCode",Md5.md5("XJJ"+Math.random()));//外部订单号，不可以重复（MD5，建议记录）
            Map<String,Object> Customer = new HashMap<String,Object>();
            Customer.put("Code",retailTianrun.getTcustmorcode());//客户编码
            sa.put("Customer",Customer);
            Map<String,Object> SettleCustomer = new HashMap<String,Object>();
            SettleCustomer.put("Code",retailTianrun.getTcustmorcode());//结算客户编码（一般等同于 客户编码）
            sa.put("SettleCustomer",SettleCustomer);
            Map<String,Object> BusinessType = new HashMap<String,Object>();
            BusinessType.put("Code","15");//业务类型编码，15–普通销售；16–销售退货
            sa.put("BusinessType",BusinessType);
            Map<String,Object> InvoiceType = new HashMap<String,Object>();
            InvoiceType.put("Code","01");//票据类型，枚举类型；00--普通发票，01--专用发票，02–收据；为空时，默认按收据处理
            sa.put("InvoiceType",InvoiceType);
            Map<String,Object> RdStyle = new HashMap<String,Object>();
            RdStyle.put("Code","201");//出库类别，RdStyleDTO对象，默认为“线上销售”类别； 具体值 我是查的数据库。
            sa.put("RdStyle",RdStyle);
            sa.put("Memo",retailTianrun.getGetdesc());//说明

            //------------------------------------------ 商品明细 只有一行 ---------------------------------------------//
            List<Map<String,Object>> SaleDeliveryDetailsList = new ArrayList<Map<String,Object>>();
            Map<String,Object> DetailM1 = new HashMap<String,Object>();
            /*Map<String,Object> DetailM1Warehouse = new HashMap<String,Object>();
            DetailM1Warehouse.put("code","0101010101");//明细1 的 仓库编码
            DetailM1.put("Warehouse",DetailM1Warehouse);*/
            Map<String,Object> DetailM1Inventory = new HashMap<String,Object>();
            DetailM1Inventory.put("code",retailTianrun.getTinventorycode());//明细1 的 存货编码
            DetailM1.put("Inventory",DetailM1Inventory);
            Map<String,Object> DetailM1Unit = new HashMap<String,Object>();
            DetailM1Unit.put("Name",retailTianrun.getUnitname());//明细1 的 存货计量单位
            DetailM1.put("Unit",DetailM1Unit);
            //DetailM1.put("Batch","？？？？？？？？？？？？？？？？？？？");//批号
            DetailM1.put("Quantity",retailTianrun.getPlansalenumbers());//明细1 的 数量
            //DetailM1.put("TaxRate",retailTianrun.getTaxnum());//明细1 的 税率
            DetailM1.put("OrigTaxPrice",retailTianrun.getTaxprice());//明细1 的 含税单价
            DetailM1.put("DetailMemo",retailTianrun.getRowmemo());//行备注

            //司机，司机身份证，司机电话，车牌号(可以用自定义字段 传入)
            List<String> biaotoulistkey = new ArrayList<String>();
            biaotoulistkey.add("pubuserdefnvc1");//司机姓名
            biaotoulistkey.add("pubuserdefnvc2");//司机车牌号
            biaotoulistkey.add("pubuserdefdecm7"); //委托数量
            biaotoulistkey.add("pubuserdefnvc6");//司机身份证号  扩展公用自定义项6
            biaotoulistkey.add("pubuserdefnvc5");//司机手机号  扩展公用自定义项5
            //biaotoulistkey.add("priuserdefnvc4"); //可以用来记录 进货单和销货单的关联字段
            List<String> biaotoulistvalue = new ArrayList<String>();
            biaotoulistvalue.add(retailTianrun.getDrivername());
            biaotoulistvalue.add(retailTianrun.getCardcode());
            biaotoulistvalue.add(retailTianrun.getPlansalenumbers());
            biaotoulistvalue.add(retailTianrun.getDrivercdcard());
            biaotoulistvalue.add(retailTianrun.getDrivermobile());
            DetailM1.put("DynamicPropertyKeys",biaotoulistkey);
            DetailM1.put("DynamicPropertyValues",biaotoulistvalue);

            SaleDeliveryDetailsList.add(DetailM1);
            //------------------------------------------ 商品明细 只有一行 ---------------------------------------------//

            sa.put("SaleDeliveryDetails",SaleDeliveryDetailsList);
            dto.put("dto",sa);
            String js = JSONObject.toJSONString(dto);
            lsresut.add(js);
        }
        return lsresut;
    }

    public static void main(String[] args) {
        Float ff = Float.valueOf("4260")/(1+(Float.valueOf("9")/100));
        System.out.println("ff ===== " + ff);
    }
}
