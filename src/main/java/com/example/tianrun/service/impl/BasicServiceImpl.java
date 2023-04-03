package com.example.tianrun.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.example.tianrun.entity.RetailTianrun;
import com.example.tianrun.mapper.orderMapper;
import com.example.tianrun.service.BasicService;
import com.example.tianrun.utils.DateUtil;
import com.example.tianrun.utils.HttpClient;
import com.example.tianrun.utils.ListToJson;
import com.example.tianrun.utils.Md5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class BasicServiceImpl implements BasicService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicServiceImpl.class);

    @Autowired
    private orderMapper orderMapper;

    @Override
    public String getResultByExcelList(List<Object> list) {
        try{
            Map<String,Object> hebingMap = new HashMap<String,Object>();//用来存取合并行号的具体内容
            List<RetailTianrun> datalist = new ArrayList<RetailTianrun>();//最终读取之后的数据
            List<RetailTianrun> sadatalist = new ArrayList<RetailTianrun>();//最终读取之后的数据
            List<Float> plansalenumberslist = new ArrayList<Float>();

            for(int i=0;i<list.size();i++){
                Object oo = list.get(i);
                RetailTianrun retailTianrun = (RetailTianrun)oo;
                Float plansalenumber = Float.valueOf(retailTianrun.getPlansalenumbers());
                plansalenumberslist.add(plansalenumber);
                String sacode = "SA-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                retailTianrun.setSacode(sacode);
                //当合并行号 不为空， 存起来
                String hebingnumber = retailTianrun.getHebingnumber();// 第 i 行的 合并行哈是   hebingnumber
                if(hebingnumber != null && !"".equals(hebingnumber)){
                    if(hebingMap.get(hebingnumber) == null || "".equals(hebingMap.get(hebingnumber).toString())){//说明 这个 合并行号 没有 写入 过
                        hebingMap.put(hebingnumber,i);
                    }else{
                        //说明存在过，我就追加在后面
                        hebingMap.put(hebingnumber,hebingMap.get(hebingnumber).toString()+","+i);
                    }
                }
                String createorderflag = retailTianrun.getCreateorderflag();//2  生成 销货单进货单  , 0  只生成进货单  ,1  只生成销货单
                sadatalist.add(retailTianrun);
                datalist.add(retailTianrun);
            }

            //再来处理这个 销售list
            Iterator<String> iterator = hebingMap.keySet().iterator();
            while (iterator.hasNext()) {
                Object key = iterator.next();
                String hebingnumbers = hebingMap.get(key).toString();  // 3,5,22  这3行是需要合并到 第一个行里
                String[] shuzu = hebingnumbers.split(",");

                int firsthang = 0;
                String totalnumber = "0";
                for(int i=0;i<shuzu.length;i++){
                    String hang = shuzu[i];
                    totalnumber = ""+(Float.valueOf(totalnumber) + Float.valueOf(sadatalist.get(Integer.valueOf(hang)).getPlansalenumbers())) ;
                    if(i==0){
                        firsthang = Integer.valueOf(hang);
                        //sadatalist.get(firsthang).setCreateorderflag("2");//2  生成 销货单 /  进货单
                    }else{
                        sadatalist.get(Integer.valueOf(hang)).setCreateorderflag("3");// 1只生成销货单 ,  0只生成进货单 , 3  就是不能生成销货单，也不能生成进货单！
                    }
                }
                //需要把 这个 totalnumber （就是 合并后的数量总和） 写入 到 第一个行 号里面的 发货数量。
                sadatalist.get(firsthang).setPlansalenumbers(""+Float.valueOf(totalnumber));
            }

            // 业务处理逻辑
            if(datalist != null && datalist.size() != 0){
                //同时 处理了  进货list  和   销货List
                String pufailstr = "";//记录失败的行数 和原因
                String safailstr = "";//记录失败的行数 和原因
                for (int i=0;i<datalist.size();i++){
                    RetailTianrun retailTianrun = datalist.get(i);

                    String createorderflag = retailTianrun.getCreateorderflag();//2  生成 销货单进货单  , 0  只生成进货单  ,1  只生成销货单

                    retailTianrun.setTpartencode(orderMapper.getTpartencodeByByConCode(retailTianrun.getContractcode()));//根据采购合同号查询对应的供应商编号
                    String sataxprice = sadatalist.get(i).getTaxprice();

                    Map<String,Object>  custmorMap = orderMapper.getTCustmorcodeByByJX(retailTianrun.getGetcustomer().trim());
                    if(custmorMap == null || custmorMap.get("code") == null || "".equals(custmorMap.get("code").toString())){
                        return safailstr + "," +  "第"+(i+2)+"行失败，原因是："+retailTianrun.getGetcustomer().trim()+" 未找到对应的客户档案或者结算客户档案，请检查excel！";
                    }
                    sadatalist.get(i).setTcustmorcode(custmorMap.get("code").toString());
                    sadatalist.get(i).setSettleCustomer(custmorMap.get("settleCustomerCode").toString());//结算客户编码
                    sadatalist.get(i).setTaxprice(sataxprice);

                    //交货地点——》油厂（决定是否扣包装）；包装（KG》包粕：散装》散粕） + 物料名称 ——》 这三样 一共决定  存货 以及 对应的 主单位(顺带取出 项目编号——》就是油厂名称对应的编号)
                    //项目中的自定义字段是判断是否  扣袋 还是 不扣袋
                    //加上 包装（ 50KG、70KG ） == 70KG*扣袋  ，否则 就是 散装 ，对应T+存货的品牌
                    //导入的物料名称+上一行的品牌，对应找到是哪个 T+存货
                    String excelInventoryname = retailTianrun.getInventory().replaceAll(" ","");
                    String excelPack = retailTianrun.getPacking();
                    String excelDeliveryplace = retailTianrun.getDeliveryplace();
                    if(excelInventoryname !=null && !"".equals(excelInventoryname) &&
                            (excelInventoryname.contains("（") || excelInventoryname.contains("(")) ){
                        // 说明是：   豆粕（43%）
                        excelInventoryname = excelInventoryname.replaceAll("（","");
                        excelInventoryname = excelInventoryname.replaceAll("）","");
                        excelInventoryname = excelInventoryname.replaceAll("\\(","");
                        excelInventoryname = excelInventoryname.replaceAll("\\)","");
                    }
                    if(excelPack.contains("KG")){
                        excelPack = excelPack+"*"+orderMapper.getXMkoudaiByName(excelDeliveryplace);//50KG*不扣袋
                    }
                    Map<String,Object> tinventorycode = orderMapper.getTinventorycodeByJX("%"+excelInventoryname+"%","%"+excelPack+"%",excelDeliveryplace);
                    if(tinventorycode == null || tinventorycode.get("code") == null){
                        return pufailstr + "," +  "第"+(i+2)+"行失败，原因是：未找到对应的存货档案，请检查excel！";
                    }
                    retailTianrun.setTinventorycode(tinventorycode.get("code").toString());
                    sadatalist.get(i).setTinventorycode(tinventorycode.get("code").toString());

                    //进货明细 的 来源单据单据对应的明细行ID  以及  订单上 这一行的 蛋白差！
                    Map<String,Object> pusourceVoucherDetailMap = orderMapper.getPUSourceVoucherDetailId(retailTianrun.getContractcode(),tinventorycode.get("code").toString());
                    if(pusourceVoucherDetailMap != null){
                        //retailTianrun.setSourceVoucherCode("xxxxxxxx");//如果找不到，就用 contractcode 本身
                        retailTianrun.setDepartmentCode(pusourceVoucherDetailMap.get("departmentCode").toString());//采购订单上的部门编码
                        retailTianrun.setPsersonCode(pusourceVoucherDetailMap.get("personCode").toString());//采购订单上的业务员编码
                        retailTianrun.setPusourceVoucherDetailId(pusourceVoucherDetailMap.get("id").toString());
                        retailTianrun.setDanbaicha(pusourceVoucherDetailMap.get("danbaicha").toString());
                        /*if(pusourceVoucherDetailMap.get("taxPrice") != null && !"".equals(pusourceVoucherDetailMap.get("taxPrice").toString())){
                            retailTianrun.setTaxprice(pusourceVoucherDetailMap.get("taxPrice").toString());//就用订单上的含税单价
                        }else{
                            //如果导入的不为空 就 用 导入的。否则就是0
                            if(retailTianrun.getTaxprice() == null || "".equals(retailTianrun.getTaxprice())){
                                retailTianrun.setTaxprice("0");//就用订单上的含税单价
                                sadatalist.get(i).setTaxprice("0");
                            }
                        }*/
                    }
                    //还要去 取 每一个商品的税率 和 单位
                    retailTianrun.setUnitname(tinventorycode.get("unitname").toString());
                    sadatalist.get(i).setUnitname(tinventorycode.get("unitname").toString());
                    retailTianrun.setProjectCode(tinventorycode.get("projectCode").toString());
                    retailTianrun.setTaxnum(tinventorycode.get("taxnum").toString());

                    if(pusourceVoucherDetailMap == null && ("2".equals(createorderflag) || "0".equals(createorderflag) )){
                        return pufailstr + "," +  "第"+(i+2)+"行失败，原因是：未找到对应的采购订单（合同号），请先检查单据！";
                    }

                    /*LOGGER.info("采购订单上的含税单价： ==== " + Float.valueOf(pusourceVoucherDetailMap.get("taxPrice").toString()));
                    LOGGER.info("导入excel的含税单价： ==== " + Float.valueOf(retailTianrun.getTaxprice()));
                    if( Float.valueOf(pusourceVoucherDetailMap.get("taxPrice").toString())  != Float.valueOf(retailTianrun.getTaxprice()) ){
                        LOGGER.info("真TM不相等？？？？？？？？？？？？？？？？？？？？");
                    }else{
                        LOGGER.info("相等啊 !!!!!!!!!!!!!!!!!!!!");
                    }*/


                    if(pusourceVoucherDetailMap != null && pusourceVoucherDetailMap.get("taxPrice") != null
                            && ( !(""+Float.valueOf(pusourceVoucherDetailMap.get("taxPrice").toString())).equals(""+Float.valueOf(retailTianrun.getTaxprice())) )
                            && ("2".equals(createorderflag) || "0".equals(createorderflag)) ){
                        return pufailstr + "," +  "第"+(i+2)+"行失败，原因是：导入的含税单价和采购订单上的含税单价不一致！，请先检查单据！";
                    }


                    if(pusourceVoucherDetailMap != null && pusourceVoucherDetailMap.get("deliveryplace") != null
                            && ( !pusourceVoucherDetailMap.get("deliveryplace").toString().equals(retailTianrun.getDeliveryplace()) )
                            && ("2".equals(createorderflag) || "0".equals(createorderflag)) ){
                        return pufailstr + "," +  "第"+(i+2)+"行失败，原因是：导入的油厂和系统已有订单上合同号对于的油厂不一致！，请先检查单据！";
                    }
                    //retailTianrun.setPlansalenumbers(plansalenumberslist.get(i).toString());
                    //注意 检查  合并和非合并的情况下， 数量是不是对的
                }

                //------------------------------------------------------------------------------------------------//
                //根据 excel 的内容，自动生成 进货单，都是 保存状态 ！
                String token = orderMapper.getTokenByAppKey("iiQG1E7l");// 天润的appkey
                List<String> pujsons = ListToJson.getPuJsonByList(datalist);
                for(int i=0;i<pujsons.size();i++){
                    String pujson = pujsons.get(i);
                    String createorderflag = datalist.get(i).getCreateorderflag();//生单标志
                    if("2".equals(createorderflag) || "0".equals(createorderflag)){
                        LOGGER.info("调用T+ 创建进货单 的pujson == " + pujson);
                        try{
                            String apiresult1 = HttpClient.HttpPost(
                                    "/tplus/api/v2/PurchaseArrivalOpenApi/Create",//关联采购合同
                                    pujson,
                                    "iiQG1E7l",//天润的appkey
                                    "90F5D3009C207AC487E34BD1A5254BBC",//天润的appSecret
                                    token);
                            LOGGER.info("调用T+ 创建 进货单的返回： apiresult1 == " + apiresult1);
                            JSONObject pujsonObject = JSONObject.parseObject(apiresult1);
                            String returncode = pujsonObject.getString("code"); // 0代表成功，999代表有业务异常
                            if(!"0".equals(returncode)){
                                pufailstr = pufailstr + "," + (i+2) + "行失败，原因是：" + JSONObject.parseObject(apiresult1).getString("message") ;
                            }else{
                                //更新 进货单 的 制单人和业务员都是  王丹
                                String data = pujsonObject.getString("data");
                                JSONObject dataObject = JSONObject.parseObject(data);
                                String id = dataObject.getString("ID");
                                orderMapper.updatePuCreatorAndCleakByID(id);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

                //------------------------------------------------------------------------------------------------//
                //根据 excel 的内容，自动生成 销货单 ，都是 保存状态 ！
                List<String> sajsons = ListToJson.getSaJsonByList(sadatalist);
                for(int i=0;i<sajsons.size();i++){
                    String sajson = sajsons.get(i);
                    String vouchercode = sadatalist.get(i).getSacode();
                    String createorderflag = sadatalist.get(i).getCreateorderflag();//生单标志
                    LOGGER.info("生单之前！ createorderflag === " + createorderflag );
                    if(("2".equals(createorderflag) || "1".equals(createorderflag)) && !"3".equals(createorderflag)){
                        LOGGER.info("调用T+ 创建 销货单 的sajson == " + sajson);
                        try{
                            String apiresult2 = HttpClient.HttpPost(
                                    "/tplus/api/v2/saleDelivery/Create",// 不做任何关联 （只 关联 excel 导入 生成的进货单:通过自定义项）
                                    sajson,
                                    "iiQG1E7l",//天润的appkey
                                    "90F5D3009C207AC487E34BD1A5254BBC",//天润的appSecret
                                    token);
                            LOGGER.info("调用T+ 创建 销货单的返回： apiresult2 == " + apiresult2);
                            if(apiresult2 != null && !"null".equals(apiresult2)){
                                //创建销货单失败，记录下失败的行数！并返回
                                safailstr = safailstr + "," + (i+2) + "行失败，原因是：" + JSONObject.parseObject(apiresult2).getString("message") ;
                            }else{
                                orderMapper.updateSaCreatorAndClerkByCode(vouchercode);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                if("".equals(pufailstr) && "".equals(safailstr)){
                    return "全部成功！";
                }else{
                    return "生成进货单时，第 ： " + pufailstr + " 失败，生成销货单时，第：" + safailstr + "失败";
                }
            }else{
                return "参数错误，请重试！";
            }
        }catch (Exception e){
            e.printStackTrace();
            return "程序异常，请查看开发日志！";
        }
    }


    // 销货单 审核后
    // 1. 判断 导入油厂和选单油厂是否一致（前端已完成）
    // 2. 查询当前数量 和 订单已经执行的数量 是否超过 订单总数  已经  当前单据时间 是否在 订单的有效时间内
    @Override
    public void getResultBySaParams(String code) {
        Map<String,Object> result = new HashMap<String,Object>();
        try {
            //销货单可能会选着多个销售订单 来 生单
            List<Map<String,Object>> saresultList = orderMapper.getSadetailByCode(code);
            if(saresultList != null && saresultList.size() !=0){
                for(Map<String,Object> saresult : saresultList){
                    String xsddcode = saresult.get("xsddcode").toString();//当前的单据 对应的  销售订单单据编号
                    //查询这个销售订单 后续 所有销货单（保存/审核中/审核）的数量总数，以及 这个 订单对应的 表头上的 开始日期-结束日期  。但是不包含当前销货单哈！
                    Map<String,Object> xsddmap = orderMapper.getXsddmapByCode(xsddcode);
                    if( xsddmap != null  &&  xsddmap.get("startdate") != null ){
                        String totalNumbers = xsddmap.get("totalNumbers").toString();// 这个销售订单后续所有销货单的总数量（包含当前单据的）
                        String ddNumbers = xsddmap.get("ddNumbers").toString();// 销售订单 本身的总数量
                        if(  Float.valueOf(totalNumbers) <= (Float.valueOf(ddNumbers) -5)  ){
                            result.put("code","0000");
                            result.put("msg","允许保存");
                            JSONObject job = new JSONObject(result);
                        }else{
                            //先 在这里进行 定金 的核销  xsddcode  一次把对应合同的 蓝字定金+红字定金的余额 ，一次 冲销(红字，减少了应收)。
                            String reddjje = ""+ -1*(Float.valueOf( orderMapper.getQTSYcanuseByCode(xsddcode)));
                            if(reddjje != null && !"".equals(reddjje) && Float.valueOf(reddjje) != 0){
                                String qtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                                orderMapper.addQTYSBySAStr(qtsycode,xsddcode,reddjje);
                                int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                                orderMapper.addQTYSdetailByStr(id+"",reddjje,"转回定金："+xsddcode);
                                //插入 应收应付余额明细表
                                orderMapper.addYSWLByQTYSCode(qtsycode);
                                // 更新 销货单 对应的 销售订单上的 已冲销金额
                                orderMapper.updateSaorderCX(xsddcode);
                            }
                            result.put("code","8888");
                            result.put("msg","超出订单执行数量(销货单 数量+对应订单已执行数量 后》= 订单总数量-5)! 已自动核销定金（后期再修改为 手动 选择是否！）");
                            JSONObject job = new JSONObject(result);
                        }
                    }else{
                        result.put("code","9999");
                        result.put("msg","销售订单异常，查不到对应数据！");
                        JSONObject job = new JSONObject(result);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            result.put("code","9999");
            result.put("msg","程序异常！");
            JSONObject job = new JSONObject(result);
        }
    }


    //进货单  处理 定金的逻辑
    @Override
    public void getResultByPUParams(String code) {
        Map<String,Object> result = new HashMap<String,Object>();
        try {
            //进货单 可能 选着 多个 采购订单生成
            List<Map<String,Object>> puresultList = orderMapper.getPudetailByCode(code);
            if(puresultList != null && puresultList.size() != 0){
                for(Map<String,Object> puresult : puresultList){
                    String cgddcode = puresult.get("cgddcode").toString();//当前的单据 对应的  采购订单单据编号
                    Map<String,Object> cgddmap = orderMapper.getCgddmapByCode(cgddcode);
                    if( cgddmap != null  &&  cgddmap.get("startdate") != null ){
                        String totalNumbers = cgddmap.get("totalNumbers").toString();//这个销售订单后续所有销货单的总数量(包含当前单据的)
                        String ddNumbers = cgddmap.get("ddNumbers").toString();//采购订单总数量
                        if( Float.valueOf(totalNumbers) <= (Float.valueOf(ddNumbers) -5) ){
                            result.put("code","0000");
                            result.put("msg","允许保存");
                            JSONObject job = new JSONObject(result);
                        }else{
                            //先 在这里进行 定金 的核销  cgddcode  一次把对应合同的 蓝字定金+红字定金 的 余额 ，一次 冲销(红字，减少了应收)。
                            String djje = ""+ -1*(Float.valueOf( orderMapper.getQTYFcanuseByCode(cgddcode)));
                            if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                                String qtsycode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                                orderMapper.addQTYFByPUStr(qtsycode,cgddcode,djje);
                                int id = Integer.valueOf(orderMapper.getMaxidByQTYF());
                                orderMapper.addQTYFdetailByStr(id+"",djje,"转回定金："+cgddcode);
                                //插入 应收应付余额明细表
                                orderMapper.addYSWLByQTYFCode(qtsycode);
                                //更新对应 采购订单对应的已冲销金额
                                orderMapper.updatePuorderCX(cgddcode);
                            }
                            result.put("code","8888");
                            result.put("msg","超出订单执行数量(销货单 数量+对应订单已执行数量 后》= 订单总数量-5)! 已自动核销定金（后期再修改为 手动 选择是否！）");
                            JSONObject job = new JSONObject(result);
                        }
                    }else{
                        result.put("code","9999");
                        result.put("msg","销售订单异常，查不到对应数据！");
                        JSONObject job = new JSONObject(result);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            result.put("code","9999");
            result.put("msg","程序异常！");
            JSONObject job = new JSONObject(result);
        }
    }

    @Override
    public String auqtysByCode(String code, String djje, String yn, String type) {
        if("add".equals(type) && "是".equals(yn) && djje != null && !"".equals(djje)){
            //生成的这个其他应收单的单号
            String qtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
            int recordQTRYByCode = orderMapper.getBJQTYSByCode(code);
            //增加其他应收的定金
            if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0 && recordQTRYByCode == 0){
                orderMapper.addQTYSByStr(qtsycode,code,djje);
                int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                orderMapper.addQTYSdetailByStr(id+"",djje,"扣定金："+code);
                //插入 应收应付余额明细表
                orderMapper.addYSWLByQTYSCode(qtsycode);
            }
            //已经存在其他应收，那就更新下嘛
            if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0 && recordQTRYByCode != 0){
                orderMapper.updateQTYSdetailByStr(code,djje);
                //同样，也要更新 应收应付余额明细表
                orderMapper.updateYSWLByQTYSCode(code,djje);
            }
        }
        return "";
    }


    @Override
    public String auqtyfByCode(String code, String djje, String yn, String type) {
        if("add".equals(type) && "是".equals(yn) && djje != null && !"".equals(djje)){
            //生成的这个其他应付单的单号
            String qtyfcode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
            int recordQTRYByCode = orderMapper.getQGQTYFByCode(code);
            //增加其他应付的定金
            if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0 && recordQTRYByCode == 0){
                orderMapper.addQTYFByStr(qtyfcode,code,djje);
                int id = Integer.valueOf(orderMapper.getMaxidByQTYF());
                orderMapper.addQTYFdetailByStr(id+"",djje,"扣定金："+code);
                //插入 应收应付余额明细表
                orderMapper.addYSWLByQTYFCode(qtyfcode);
            }
            //已经存在其他应收，那就更新下嘛
            if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0 && recordQTRYByCode != 0){
                orderMapper.updateQTYFdetailByStr(code,djje);
                //同样，也要更新 应收应付余额明细表
                orderMapper.updateYSWLByQTYFCode(code,djje);
            }
        }
        return "";
    }

    @Override
    public void dealQTYSBySaOrderCode(String code) {
        synchronized (this){
            //如果有来源单号（报价单单号）：先用报价单单号 生成 其他应收（红字），金额是：报价单上的定金金额*（销售订单总数量/报价单总数量）
            //再生成 一个 其他应收单（蓝字），金额是：销售订单上的 定金金额（表头上的）合同定金 + 是否生成  来 自动生成 其他应收的蓝字（合同定金 ）
            Map<String,Object> mxmap = orderMapper.getSaorderMx(code);
            String zzct = mxmap.get("zzct").toString();//中止行的数量
            String mxct = mxmap.get("mxct").toString();//明细行的数量
            if(mxct.equals(zzct)){// 说明此单是 全部行中止的！
                //先 在这里进行 定金 的核销  xsddcode  一次把对应合同的 蓝字定金+红字定金的余额 ，一次 冲销(红字，减少了应收)。
                String reddjje = ""+ -1*(Float.valueOf( orderMapper.getQTSYcanuseByCode(code)));
                if(reddjje != null && !"".equals(reddjje) && Float.valueOf(reddjje) != 0){
                    String qtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);

                    orderMapper.addQTYSBySAStr(qtsycode,code,reddjje);
                    int id = Integer.valueOf(orderMapper.getMaxidByQTYS());

                    orderMapper.addQTYSdetailByStr(id+"",reddjje,"转回定金："+code);

                    //插入 应收应付余额明细表
                    orderMapper.addYSWLByQTYSCode(qtsycode);

                    // 更新 销货单 对应的 销售订单上的 已冲销金额
                    orderMapper.updateSaorderCX(code);
                }
            }else{
                Map<String,Object> params = orderMapper.getSaorderDetailByCode(code);
                //就是 普通 订单的正常 变更保存，需要 红冲，再蓝字什么的
                if(params != null && params.get("idsourcevouchertype") != null && !"".equals(params.get("idsourcevouchertype").toString())
                        && "103".equals(params.get("idsourcevouchertype").toString())){
                    //说明 来源单价是 报价单哦
                    String bjcode = params.get("SourceVoucherCode").toString();//报价单单号
                    int ct = orderMapper.getRecordQTYSByCode(bjcode,code);//系统中 是否已经有对应的其他应收单
                    String bjct = params.get("bjct").toString();//报价单总数量
                    String bjdjje = params.get("bjdjje").toString();//报价单总金额
                    String sact = params.get("sact").toString();//销售订单总数量
                    String sadjje = params.get("pubuserdefdecm1").toString();//销售订单上的 定金金额（表头上的）合同定金
                    //是否是 某人，且 是否 要 生成定金！ 麻烦把采购环节（请购和采购订单）中生成订金的操作放给王丹。销售的放给叶新蓉
                    if(params.get("yn")!=null && "是".equals(params.get("yn").toString())
                            && params.get("bgr") != null && !"".equals(params.get("bgr").toString())
                            && "叶新蓉".equals(params.get("bgr").toString()) && ct == 0){
                        //先增加其他应收的定金(红字！)  合同号 用 来源单号
                        String redqtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                        String reddjje =  ""+(-1f * ( Float.valueOf(bjdjje) * (Float.valueOf(sact)/Float.valueOf(bjct))));
                        if(reddjje != null && !"".equals(reddjje) && Float.valueOf(reddjje) != 0){
                            orderMapper.addREDQTYSBySAStr(redqtsycode,code, reddjje);
                            int redid = Integer.valueOf(orderMapper.getMaxidByQTYS());
                            orderMapper.addQTYSdetailByStr(redid+"",reddjje,"转回定金：" + bjcode);
                            //插入 应收应付余额明细表
                            orderMapper.addYSWLByQTYSCode(redqtsycode);
                            //再更新下 对应的 报价单上的 已冲销金额
                            orderMapper.updateSaleQuotationCX(bjcode);
                        }
                        //再生成蓝字的QTYS
                        String qtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                        //增加其他应收的定金
                        if(sadjje != null && !"".equals(sadjje) && Float.valueOf(sadjje) != 0){
                            orderMapper.addQTYSBySAStr(qtsycode,code,sadjje);
                            int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                            orderMapper.addQTYSdetailByStr(id+"",sadjje,"扣定金："+code);
                            //插入 应收应付余额明细表
                            orderMapper.addYSWLByQTYSCode(qtsycode);
                        }
                    }
                    // 已经存在，就只能 更新！！！
                    if(params.get("yn")!=null && "是".equals(params.get("yn").toString())
                            && params.get("bgr") != null && !"".equals(params.get("bgr").toString())
                            && "叶新蓉".equals(params.get("bgr").toString()) && ct != 0){
                        //先 更新 其他应收的定金(红字！)
                        String reddjje =  ""+(-1f * ( Float.valueOf(bjdjje) * (Float.valueOf(sact)/Float.valueOf(bjct))));
                        if(reddjje != null && !"".equals(reddjje) && Float.valueOf(reddjje) != 0){
                            orderMapper.updateRedQTYSdetailByStr(bjcode,code,reddjje);
                            orderMapper.updateRedYSWLByQTYSCode(bjcode,code,reddjje);
                        }
                        //再 更新 蓝字的其他应收的定金
                        orderMapper.updateQTYSdetailByStr(code,sadjje);
                        orderMapper.updateYSWLByQTYSCode(code,sadjje);
                    }
                }else{
                    int ct = orderMapper.getRecordQTYSByDDCode(code);
                    //是否是 某人，且 是否 要 生成定金！ 麻烦把采购环节（请购和采购订单）中生成订金的操作放给王丹。销售的放给叶新蓉
                    if(params.get("yn")!=null && "是".equals(params.get("yn").toString())
                            && params.get("bgr") != null && !"".equals(params.get("bgr").toString())
                            && "叶新蓉".equals(params.get("bgr").toString()) && ct == 0){
                        //如果没有来源单号，且系统中也不存在对应的其他应收单， 则直接生成 QTYS
                        String qtyscode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                        String djje = params.get("pubuserdefdecm1").toString();
                        //增加其他应收的定金
                        if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                            orderMapper.addQTYSBySAStr(qtyscode,code,djje);
                            int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                            orderMapper.addQTYSdetailByStr(id+"",djje,"扣定金："+code);
                            //插入 应收应付余额明细表
                            orderMapper.addYSWLByQTYSCode(qtyscode);
                        }
                    }
                    if(params.get("yn")!=null && "是".equals(params.get("yn").toString())
                            && params.get("bgr") != null && !"".equals(params.get("bgr").toString())
                            && "叶新蓉".equals(params.get("bgr").toString()) && ct != 0){
                        //就只能更新了！！！！
                        String djje = params.get("pubuserdefdecm1").toString();
                        orderMapper.updateQTYSdetailByStr(code,djje);
                        orderMapper.updateYSWLByQTYSCode(code,djje);
                    }
                }
            }
        }
    }

    @Override
    public void dealQTYFByPuOrderCode(String code) {
        synchronized (this){
            Map<String,Object> mxmap = orderMapper.getPuorderMx(code);
            String zzct = mxmap.get("zzct").toString();//中止行的数量
            String mxct = mxmap.get("mxct").toString();//明细行的数量
            if(mxct.equals(zzct)) {// 说明此单是 全部行中止的！
                //先 在这里进行 定金 的核销  cgddcode  一次把对应合同的 蓝字定金+红字定金 的 余额 ，一次 冲销(红字，减少了应收)。
                String djje = ""+ -1*(Float.valueOf( orderMapper.getQTYFcanuseByCode(code)));
                if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                    String qtsycode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                    orderMapper.addQTYFByPUStr(qtsycode,code,djje);
                    int id = Integer.valueOf(orderMapper.getMaxidByQTYF());
                    orderMapper.addQTYFdetailByStr(id+"",djje,"转回定金："+code);
                    //插入 应收应付余额明细表
                    orderMapper.addYSWLByQTYFCode(qtsycode);
                    //更新对应 采购订单对应的已冲销金额
                    orderMapper.updatePuorderCX(code);
                }
            }else{
                Map<String,Object> params = orderMapper.getPuorderDetailByCode(code);//此处查询 采购的 单据情况
                if(params != null && params.get("idsourcevouchertype") != null && !"".equals(params.get("idsourcevouchertype").toString())
                        && "101".equals(params.get("idsourcevouchertype").toString())){
                    //说明 来源单价是 请购单
                    String qgcode = params.get("SourceVoucherCode").toString();//请购单单号
                    int ct = orderMapper.getRecordQTYFByCode(qgcode,code);//系统中 是否已经有对应的其他应付单()
                    String bjct = params.get("bjct").toString();//请购单总数量
                    String bjdjje = params.get("bjdjje").toString();//请购单总金额
                    String sact = params.get("sact").toString();//采购订单总数量
                    String sadjje = params.get("pubuserdefdecm1").toString();//采购订单上的 定金金额（表头上的）合同定金
                    //是否是 某人，且 是否 要 生成定金！ 麻烦把采购环节（请购和采购订单）中生成订金的操作放给王丹。销售的放给叶新蓉
                    if(params.get("yn")!=null && "是".equals(params.get("yn").toString())
                            && params.get("bgr") != null && !"".equals(params.get("bgr").toString())
                            && "王丹".equals(params.get("bgr").toString()) && ct == 0){
                        //先增加其他应收的定金(红字！)  但是 合同号 要用 来源单据编号
                        String redqtsycode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                        String reddjje =  ""+(-1f * ( Float.valueOf(bjdjje) * (Float.valueOf(sact)/Float.valueOf(bjct))));
                        LOGGER.info("reddjje ============== " + reddjje);
                        if(reddjje != null && !"".equals(reddjje) && Float.valueOf(reddjje) != 0){
                            orderMapper.addREDQTYFByPUStr(redqtsycode,code, reddjje);
                            int redid = Integer.valueOf(orderMapper.getMaxidByQTYF());
                            orderMapper.addQTYFdetailByStr(redid+"",reddjje,"转回定金："+qgcode);
                            //插入 应收应付余额明细表
                            orderMapper.addYSWLByQTYFCode(redqtsycode);
                            // 需要再更新下对应的 请购单上的 已冲销金额
                            orderMapper.updatePurchaseRequisitionCX(qgcode);
                        }
                        //再生成蓝字的QTYS
                        String qtsycode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                        //增加其他应收的定金
                        if(sadjje != null && !"".equals(sadjje) && Float.valueOf(sadjje) != 0){
                            orderMapper.addQTYFByPUStr(qtsycode,code,sadjje);
                            int id = Integer.valueOf(orderMapper.getMaxidByQTYF());
                            orderMapper.addQTYFdetailByStr(id+"",sadjje,"扣定金："+code);
                            //插入 应收应付余额明细表
                            orderMapper.addYSWLByQTYFCode(qtsycode);
                        }
                    }
                    //只能更新！
                    if(params.get("yn")!=null && "是".equals(params.get("yn").toString())
                            && params.get("bgr") != null && !"".equals(params.get("bgr").toString())
                            && "王丹".equals(params.get("bgr").toString()) && ct != 0){
                        //先 更新 其他应付的定金(红字！)
                        String reddjje =  ""+(-1f * ( Float.valueOf(bjdjje) * (Float.valueOf(sact)/Float.valueOf(bjct))));
                        if(reddjje != null && !"".equals(reddjje) && Float.valueOf(reddjje) != 0){
                            orderMapper.updateRedQTYFdetailByStr(qgcode,code,reddjje);
                            orderMapper.updateRedYSWLByQTYFCode(qgcode,code,reddjje);
                        }
                        //再 更新 蓝字的其他应付的定金
                        orderMapper.updateQTYFdetailByStr(code,sadjje);
                        orderMapper.updateYSWLByQTYFCode(code,sadjje);
                    }
                }else{
                    int ct = orderMapper.getRecordQTYFByDDCode(code);
                    // 没有来源单号
                    // 是否是 某人，且 是否 要 生成定金！ 麻烦把采购环节（请购和采购订单）中生成订金的操作放给王丹。销售的放给叶新蓉
                    if(params.get("yn")!=null && "是".equals(params.get("yn").toString())
                            && params.get("bgr") != null && !"".equals(params.get("bgr").toString())
                            && "王丹".equals(params.get("bgr").toString()) && ct ==0 ){
                        //如果没有来源单号，直接生成 QTYS
                        String qtyscode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                        String djje = params.get("pubuserdefdecm1").toString();
                        //增加其他应收的定金
                        if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                            orderMapper.addQTYFByPUStr(qtyscode,code,djje);
                            int id = Integer.valueOf(orderMapper.getMaxidByQTYF());
                            orderMapper.addQTYFdetailByStr(id+"",djje,"扣定金："+code);
                            //插入 应收应付余额明细表
                            orderMapper.addYSWLByQTYFCode(qtyscode);
                        }
                    }
                    if(params.get("yn")!=null && "是".equals(params.get("yn").toString())
                            && params.get("bgr") != null && !"".equals(params.get("bgr").toString())
                            && "王丹".equals(params.get("bgr").toString()) && ct !=0 ){
                        String djje = params.get("pubuserdefdecm1").toString();
                        orderMapper.updateQTYFdetailByStr(code,djje);
                        orderMapper.updateYSWLByQTYFCode(code,djje);
                    }
                }
            }
        }
    }
}