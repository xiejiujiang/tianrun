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
                        sadatalist.get(firsthang).setCreateorderflag("2");//2  生成 销货单 /  进货单
                    }else{
                        sadatalist.get(Integer.valueOf(hang)).setCreateorderflag("0");// 1只生成销货单 ,  0只生成进货单
                    }
                }
                //需要把 这个 totalnumber （就是 合并后的数量总和） 写入 到 第一个行 号里面的 发货数量。最后统一 删掉 不要的
                sadatalist.get(firsthang).setPlansalenumbers(""+Float.valueOf(totalnumber));
            }

            // 业务处理逻辑
            if(datalist != null && datalist.size() != 0){
                //同时 处理了  进货list  和   销货List
                String pufailstr = "";//记录失败的行数 和原因
                String safailstr = "";//记录失败的行数 和原因
                for (int i=0;i<datalist.size();i++){
                    RetailTianrun retailTianrun = datalist.get(i);
                    retailTianrun.setTpartencode(orderMapper.getTpartencodeByByConCode(retailTianrun.getContractcode()));//根据采购合同号查询对应的供应商编号
                    String sataxprice = sadatalist.get(i).getTaxprice();
                    sadatalist.get(i).setTcustmorcode(orderMapper.getTCustmorcodeByByJX(retailTianrun.getGetcustomer()));

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
                    retailTianrun.setTinventorycode(tinventorycode.get("code").toString());
                    sadatalist.get(i).setTinventorycode(tinventorycode.get("code").toString());

                    //进货明细 的 来源单据单据对应的明细行ID  以及  订单上 这一行的 蛋白差！
                    Map<String,Object> pusourceVoucherDetailMap = orderMapper.getPUSourceVoucherDetailId(retailTianrun.getContractcode(),tinventorycode.get("code").toString());
                    if(pusourceVoucherDetailMap!=null){
                        //retailTianrun.setSourceVoucherCode("xxxxxxxx");//如果找不到，就用 contractcode 本身
                        retailTianrun.setPusourceVoucherDetailId(pusourceVoucherDetailMap.get("id").toString());
                        retailTianrun.setDanbaicha(pusourceVoucherDetailMap.get("danbaicha").toString());
                        if(pusourceVoucherDetailMap.get("taxPrice") != null && !"".equals(pusourceVoucherDetailMap.get("taxPrice").toString())){
                            retailTianrun.setTaxprice(pusourceVoucherDetailMap.get("taxPrice").toString());//就用订单上的含税单价
                        }else{
                            //如果导入的不为空 就 用 导入的。否则就是0
                            if(retailTianrun.getTaxprice() == null || "".equals(retailTianrun.getTaxprice())){
                                retailTianrun.setTaxprice("0");//就用订单上的含税单价
                                sadatalist.get(i).setTaxprice("0");
                            }
                        }
                        //还要去 取 每一个商品的税率 和 单位
                        retailTianrun.setUnitname(tinventorycode.get("unitname").toString());
                        sadatalist.get(i).setUnitname(tinventorycode.get("unitname").toString());
                        retailTianrun.setProjectCode(tinventorycode.get("projectCode").toString());
                        retailTianrun.setTaxnum(tinventorycode.get("taxnum").toString());// 9  taxnum
                    }else{
                        return pufailstr + "," +  "第"+(i+2)+"行失败，原因是：未找到对应的采购订单（合同号），请先检查单据！";
                    }
                    sadatalist.get(i).setTaxprice(sataxprice);
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
                    String createorderflag = sadatalist.get(i).getCreateorderflag();//生单标志
                    if("2".equals(createorderflag) || "1".equals(createorderflag)){
                        LOGGER.info("调用T+ 创建 销货单 的sajson == " + sajson);
                        try{
                            String apiresult2 = HttpClient.HttpPost(
                                    "/tplus/api/v2/saleDelivery/Create",// 不做任何关联 （只 关联 excel 导入 生成的进货单:通过自定义项）
                                    sajson,
                                    "iiQG1E7l",//天润的appkey
                                    "90F5D3009C207AC487E34BD1A5254BBC",//天润的appSecret
                                    token);
                            LOGGER.info("调用T+ 创建 销货单的返回： apiresult2 == " + apiresult2);

                            //如果这个销货单 创建 成功，且这个 客户 有 预收款 ，那就按 当前的 销售含税金额 进行 自动生成 使用预收的明细
                            //客户 有 多笔 预收款时。 销货单使用预收是 按 时间 先后顺序 进行冲抵，有余额就冲（但是 要考虑到 押金转预收 再冲抵的情况）
                            if(apiresult2 == null || "null".equals(apiresult2)){
                                //由于 现在 销货单不会再使用预收了，天润的系统里面根本就不会有预收款了！
                                /*RetailTianrun retailTianrun = sadatalist.get(i);//拿到对应的 原始参数
                                String tcustmorcode = retailTianrun.getTcustmorcode();//客户编号
                                List<Map<String,Object>> custmoryushoulist = orderMapper.getCustmoryushouByCode(tcustmorcode);
                                //只有一个预收款
                                if(custmoryushoulist != null && custmoryushoulist.size() == 1
                                        && custmoryushoulist.get(0).get("code") != null
                                        && !"NULL".equals(custmoryushoulist.get(0).get("code").toString())){
                                    Map<String,Object> custmoryushou = custmoryushoulist.get(0);
                                    Float amount = Float.valueOf(custmoryushou.get("amount").toString());
                                    if(amount >= (Float.valueOf(retailTianrun.getPlansalenumbers()) * Float.valueOf(retailTianrun.getTaxprice()))){
                                        custmoryushou.put("cancelamount",Float.valueOf(retailTianrun.getPlansalenumbers()) * Float.valueOf(retailTianrun.getTaxprice()));
                                        custmoryushou.put("sacode",sacode);
                                        orderMapper.updateSAYuShou(custmoryushou);
                                        orderMapper.addSAYuShou(custmoryushou);// 这个 日期 要 改成 预收款的 日期
                                    }
                                }
                                //有多个预收款!
                                if(custmoryushoulist != null && custmoryushoulist.size() > 1
                                        && custmoryushoulist.get(0).get("code") != null
                                        && !"NULL".equals(custmoryushoulist.get(0).get("code").toString())){
                                    String totalyushoAmount = orderMapper.getTotalYushouAmountByCode(tcustmorcode);
                                    if(Float.valueOf(totalyushoAmount) >= (Float.valueOf(retailTianrun.getPlansalenumbers()) * Float.valueOf(retailTianrun.getTaxprice()))){
                                        //说明 有 足够的 预收款 可以用！（只不过是 多个预收组合的）
                                        Float satotalAmount = Float.valueOf(retailTianrun.getPlansalenumbers()) * Float.valueOf(retailTianrun.getTaxprice());
                                        List<Map<String,Object>> insertyushoulist = new ArrayList<Map<String,Object>>();
                                        for(int ii = 0 ; ii < custmoryushoulist.size(); ii ++){
                                            Map<String,Object> custmoryushou = custmoryushoulist.get(ii);
                                            custmoryushou.put("sacode",sacode);
                                            String yushouAmount = custmoryushou.get("amount").toString();
                                            if(satotalAmount >= Float.valueOf(yushouAmount)){
                                                custmoryushou.put("cancelamount",""+Float.valueOf(yushouAmount));
                                                insertyushoulist.add(custmoryushou);//第一个 预收款的 明细 需要放进去
                                            }else{
                                                custmoryushou.put("cancelamount",""+satotalAmount);
                                                insertyushoulist.add(custmoryushou);//第一个 预收款的 明细 需要放进去
                                                break;
                                            }
                                            satotalAmount = satotalAmount - Float.valueOf(yushouAmount);
                                        }
                                        //再向销货单预收使用明细表中 插入 这个List : insertyushoulist
                                        orderMapper.addSAYuShouList(insertyushoulist);
                                        //更新 销货单上的 表头上的  预收金额 （不是 销货单预收使用明细哈！）
                                        Map<String,Object> mapyushou = new HashMap<String,Object>();
                                        mapyushou.put("cancelamount",Float.valueOf(retailTianrun.getPlansalenumbers()) * Float.valueOf(retailTianrun.getTaxprice()));
                                        mapyushou.put("sacode",sacode);
                                        orderMapper.updateSAYuShou(mapyushou);
                                    }
                                }*/
                            }else{
                                //创建销货单失败，记录下失败的行数！并返回
                                safailstr = safailstr + "," + (i+2) + "行失败，原因是：" + JSONObject.parseObject(apiresult2).getString("message") ;
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


    // 销货单 保存后 的时候
    // 1. 判断 导入油厂和选单油厂是否一致（前端已完成）
    // 2. 查询当前数量 和 订单已经执行的数量 是否超过 订单总数  已经  当前单据时间 是否在 订单的有效时间内
    // 放弃！ 3. 如果 客户的 自定义项 是 先款，还需与判断 还能使用的预收款金额 》=  此单的销售金额 。否则，不卖！
    // 4. 如果上面三点都没问题才允许保存！ （再 通过 保存的消息订阅 来更新明细里面的  司机 4 个信息）
    @Override
    public String getResultBySaParams(String code) {
        Map<String,Object> result = new HashMap<String,Object>();
        try {
            //根据 销货单的单号，查询 所有关联信息
            Map<String,Object> saresult = orderMapper.getSadetailByCode(code);//就为了获取下面3个，真TM SB 代码！
            String xsddcode = saresult.get("xsddcode").toString();//当前的单据 对应的  销售订单单据编号
            String codeday = saresult.get("codeday").toString();//当前的 单据日期
            String numbers = saresult.get("numbers").toString();//当前单 据的总数量
            //查询这个销售订单 后续 所有销货单（保存/审核中/审核）的数量总数，以及 这个 订单对应的 表头上的 开始日期-结束日期  。但是不包含当前销货单哈！
            //表头上 这个地方  选单之后 是空白的！！！
            Map<String,Object> xsddmap = orderMapper.getXsddmapByCode(xsddcode,code);
            if( xsddmap != null  &&  xsddmap.get("startdate") != null ){
                String totalNumbers = xsddmap.get("totalNumbers").toString();//这个销售订单后续所有销货单的总数量
                //这个只能 单独 去查一次！
                String ddNumbers = orderMapper.getddNumbersByCode(xsddcode); //xsddmap.get("ddNumbers").toString();// 销售订单 本身的总数量
                String starteDate = xsddmap.get("startdate").toString();// 销售订单 表头 的开始时间
                String enddate = xsddmap.get("enddate").toString();//  销售订单 表头 的结束时间
                if(DateUtil.isEffectiveDate(new SimpleDateFormat("yyyyMMdd").parse(codeday),
                        new SimpleDateFormat("yyyyMMdd").parse(starteDate),
                        new SimpleDateFormat("yyyyMMdd").parse(enddate))
                        && (Float.valueOf(numbers) + Float.valueOf(totalNumbers)) <= Float.valueOf(ddNumbers) -5 ){
                    result.put("code","0000");
                    result.put("msg","允许保存");
                    JSONObject job = new JSONObject(result);
                    return job.toJSONString();
                }else{
                    if( (Float.valueOf(numbers) + Float.valueOf(totalNumbers)) > Float.valueOf(ddNumbers) -5 ){
                        //先 在这里进行 定金 的核销  xsddcode  一次把对应合同的 蓝字定金+红字定金的余额 ，一次 冲销(红字，减少了应收)。
                        String djje = ""+ -1*(Float.valueOf( orderMapper.getQTSYcanuseByCode(xsddcode)));
                        if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                            String qtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                            orderMapper.addQTYSBySAStr(qtsycode,xsddcode,djje);
                            int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                            orderMapper.addQTYSdetailByStr(id+"",djje);
                            //插入 应收应付余额明细表
                            orderMapper.addYSWLByQTYSCode(qtsycode);
                        }
                        result.put("code","8888");
                        result.put("msg","超出订单执行数量(销货单 数量+对应订单已执行数量 后》= 订单总数量-5)! 已自动核销定金（后期再修改为 手动 选择是否！）");
                        JSONObject job = new JSONObject(result);
                        return job.toJSONString();
                    }
                    if(!DateUtil.isEffectiveDate(new SimpleDateFormat("yyyyMMdd").parse(codeday),
                            new SimpleDateFormat("yyyyMMdd").parse(starteDate),
                            new SimpleDateFormat("yyyyMMdd").parse(enddate))){
                        result.put("code","8888");
                        result.put("msg","超出订单执行时间范围!在备注中填入超时两个字，进入审批流！");
                        JSONObject job = new JSONObject(result);
                        return job.toJSONString();
                    }else{
                        result.put("code","9999");
                        result.put("msg","不允许保存！");
                        JSONObject job = new JSONObject(result);
                        return job.toJSONString();
                    }
                }
            }else{
                result.put("code","9999");
                result.put("msg","销售订单异常，查不到对应数据！");
                JSONObject job = new JSONObject(result);
                return job.toJSONString();
            }
        }catch (Exception e){
            e.printStackTrace();
            result.put("code","9999");
            result.put("msg","程序异常！");
            JSONObject job = new JSONObject(result);
            return job.toJSONString();
        }
    }


    //进货单  处理 定金的逻辑
    @Override
    public String getResultByPUParams(String code) {
        Map<String,Object> result = new HashMap<String,Object>();
        try {
            //根据 进货单的单号，查询 所有关联信息
            Map<String,Object> saresult = orderMapper.getPudetailByCode(code);//就为了获取下面3个，真TM SB 代码！
            String cgddcode = saresult.get("cgddcode").toString();//当前的单据 对应的  采购订单单据编号
            String codeday = saresult.get("codeday").toString();//当前的 单据日期
            String numbers = saresult.get("numbers").toString();//当前单 据的总数量

            Map<String,Object> xsddmap = orderMapper.getCgddmapByCode(cgddcode,code);
            if( xsddmap != null  &&  xsddmap.get("startdate") != null ){
                String totalNumbers = xsddmap.get("totalNumbers").toString();//这个销售订单后续所有销货单的总数量
                //这个只能 单独 去查一次！
                String ddNumbers = orderMapper.getcgNumbersByCode(cgddcode);
                String starteDate = xsddmap.get("startdate").toString();// 销售订单 表头 的开始时间
                String enddate = xsddmap.get("enddate").toString();//  销售订单 表头 的结束时间
                if(DateUtil.isEffectiveDate(new SimpleDateFormat("yyyyMMdd").parse(codeday),
                        new SimpleDateFormat("yyyyMMdd").parse(starteDate),
                        new SimpleDateFormat("yyyyMMdd").parse(enddate))
                        && (Float.valueOf(numbers) + Float.valueOf(totalNumbers)) <= Float.valueOf(ddNumbers) -5 ){
                    result.put("code","0000");
                    result.put("msg","允许保存");
                    JSONObject job = new JSONObject(result);
                    return job.toJSONString();
                }else{
                    if( (Float.valueOf(numbers) + Float.valueOf(totalNumbers)) > Float.valueOf(ddNumbers) -5 ){
                        //先 在这里进行 定金 的核销  xsddcode  一次把对应合同的 蓝字定金+红字定金的余额 ，一次 冲销(红字，减少了应收)。
                        String djje = ""+ -1*(Float.valueOf( orderMapper.getQTYFcanuseByCode(cgddcode)));
                        if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                            String qtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                            orderMapper.addQTYFByPUStr(qtsycode,cgddcode,djje);
                            int id = Integer.valueOf(orderMapper.getMaxidByQTYF());
                            orderMapper.addQTYFdetailByStr(id+"",djje);
                            //插入 应收应付余额明细表
                            orderMapper.addYSWLByQTYFCode(qtsycode);
                        }
                        result.put("code","8888");
                        result.put("msg","超出订单执行数量(销货单 数量+对应订单已执行数量 后》= 订单总数量-5)! 已自动核销定金（后期再修改为 手动 选择是否！）");
                        JSONObject job = new JSONObject(result);
                        return job.toJSONString();
                    }
                    if(!DateUtil.isEffectiveDate(new SimpleDateFormat("yyyyMMdd").parse(codeday),
                            new SimpleDateFormat("yyyyMMdd").parse(starteDate),
                            new SimpleDateFormat("yyyyMMdd").parse(enddate))){
                        result.put("code","8888");
                        result.put("msg","超出订单执行时间范围!在备注中填入超时两个字，进入审批流！");
                        JSONObject job = new JSONObject(result);
                        return job.toJSONString();
                    }else{
                        result.put("code","9999");
                        result.put("msg","不允许保存！");
                        JSONObject job = new JSONObject(result);
                        return job.toJSONString();
                    }
                }
            }else{
                result.put("code","9999");
                result.put("msg","销售订单异常，查不到对应数据！");
                JSONObject job = new JSONObject(result);
                return job.toJSONString();
            }
        }catch (Exception e){
            e.printStackTrace();
            result.put("code","9999");
            result.put("msg","程序异常！");
            JSONObject job = new JSONObject(result);
            return job.toJSONString();
        }
    }

    @Override
    public String auqtysByCode(String code, String djje, String yn, String type) {
        if("add".equals(type) && "是".equals(yn) && djje != null && !"".equals(djje)){
            //生成的这个其他应收单的单号
            String qtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
            //增加其他应收的定金
            if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                orderMapper.addQTYSByStr(qtsycode,code,djje);
                int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                orderMapper.addQTYSdetailByStr(id+"",djje);
                //插入 应收应付余额明细表
                orderMapper.addYSWLByQTYSCode(qtsycode);
            }
        }else{//实际上 现在 没有这个了！！！  如果生成多了，让客户自己去 删除！
            //直接 删除 其他应收单（priuserdefnvc1 是 code）
            orderMapper.deleteQTYSdetail(code);
            orderMapper.deleteQTYS(code);
            //删除 应收应付余额明细表
            orderMapper.deleteYSWLByQTYSCode(code);
        }
        return "";
    }


    @Override
    public String auqtyfByCode(String code, String djje, String yn, String type) {
        if("add".equals(type) && "是".equals(yn) && djje != null && !"".equals(djje)){
            //生成的这个其他应付单的单号
            String qtyfcode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
            //增加其他应付的定金
            if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                orderMapper.addQTYFByStr(qtyfcode,code,djje);
                int id = Integer.valueOf(orderMapper.getMaxidByQTYF());
                orderMapper.addQTYFdetailByStr(id+"",djje);
                //插入 应收应付余额明细表
                orderMapper.addYSWLByQTYFCode(qtyfcode);
            }
        }else{//实际上 现在 没有这个了！！！  如果生成多了，让客户自己去 删除！
            //直接 删除 其他应付单（priuserdefnvc1 是 code）
            //orderMapper.deleteQTYFdetail(code);
            //orderMapper.deleteQTYF(code);
            //删除 应收应付余额明细表
            //orderMapper.deleteYSWLByQTYFCode(code);
        }
        return "";
    }

    @Override
    public void dealQTYSBySaOrderCode(String code) {
        synchronized (this){
            //如果有来源单号（报价单单号）：先用报价单单号 生成 其他应收（红字），金额是：报价单上的定金金额*（销售订单总数量/报价单总数量）
            //再生成 一个 其他应收单（蓝字），金额是：销售订单上的 定金金额（表头上的）合同定金 + 是否生成  来 自动生成 其他应收的蓝字（合同定金 ）
            Map<String,Object> params = orderMapper.getSaorderDetailByCode(code);
            if(params.get("idsourcevouchertype") != null && !"".equals(params.get("idsourcevouchertype").toString())
                    && "103".equals(params.get("idsourcevouchertype").toString())){//说明 来源单价是 报价单哦
                String bjcode = params.get("SourceVoucherCode").toString();//报价单单号
                String bjct = params.get("bjct").toString();//报价单总数量
                String bjdjje = params.get("bjdjje").toString();//报价单总数量
                String sact = params.get("sact").toString();//销售订单总数量
                String sadjje = params.get("pubuserdefdecm1").toString();//销售订单上的 定金金额（表头上的）合同定金
                //String yn = params.get("yn").toString();//销售订单上的 是否生成  来 自动生成 其他应收的蓝字
                if(params.get("yn")!=null && "是".equals(params.get("yn").toString())){
                    //先增加其他应收的定金(红字！)
                    String redqtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                    String reddjje =  ""+(-1f * ( Float.valueOf(bjdjje) * (Float.valueOf(sact)/Float.valueOf(bjct))));
                    if(reddjje != null && !"".equals(reddjje) && Float.valueOf(reddjje) != 0){
                        orderMapper.addQTYSBySAStr(redqtsycode,code, reddjje);
                        int redid = Integer.valueOf(orderMapper.getMaxidByQTYS());
                        orderMapper.addQTYSdetailByStr(redid+"",reddjje);
                        //插入 应收应付余额明细表
                        orderMapper.addYSWLByQTYSCode(redqtsycode);
                    }
                    //再生成蓝字的QTYS
                    String qtsycode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                    //增加其他应收的定金
                    if(sadjje != null && !"".equals(sadjje) && Float.valueOf(sadjje) != 0){
                        orderMapper.addQTYSBySAStr(qtsycode,code,sadjje);
                        int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                        orderMapper.addQTYSdetailByStr(id+"",sadjje);
                        //插入 应收应付余额明细表
                        orderMapper.addYSWLByQTYSCode(qtsycode);
                    }
                }
            }else{
                if(params.get("yn")!=null && "是".equals(params.get("yn").toString())){
                    //如果没有来源单号，直接生成 QTYS
                    String qtyscode = "QTYS-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                    String djje = params.get("pubuserdefdecm1").toString();
                    //增加其他应收的定金
                    if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                        orderMapper.addQTYSBySAStr(qtyscode,code,djje);
                        int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                        orderMapper.addQTYSdetailByStr(id+"",djje);
                        //插入 应收应付余额明细表
                        orderMapper.addYSWLByQTYSCode(qtyscode);
                    }
                }
            }
        }
    }

    @Override
    public void dealQTYFBySaOrderCode(String code) {
        synchronized (this){
            Map<String,Object> params = orderMapper.getSaorderDetailByCode(code);
            if(params.get("idsourcevouchertype") != null && !"".equals(params.get("idsourcevouchertype").toString())
                    && "103".equals(params.get("idsourcevouchertype").toString())){//说明 来源单价是 请购单
                String bjcode = params.get("SourceVoucherCode").toString();//请购单单号
                String bjct = params.get("bjct").toString();//请购单总数量
                String bjdjje = params.get("bjdjje").toString();//请购单总金额
                String sact = params.get("sact").toString();//采购订单总数量
                String sadjje = params.get("pubuserdefdecm1").toString();//采购订单上的 定金金额（表头上的）合同定金
                //String yn = params.get("yn").toString();//采购订单上的 是否生成  来 自动生成 其他应付的蓝字
                if(params.get("yn")!=null && "是".equals(params.get("yn").toString())){
                    //先增加其他应收的定金(红字！)
                    String redqtsycode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                    String reddjje =  ""+(-1f * ( Float.valueOf(bjdjje) * (Float.valueOf(sact)/Float.valueOf(bjct))));
                    if(reddjje != null && !"".equals(reddjje) && Float.valueOf(reddjje) != 0){
                        orderMapper.addQTYFByPUStr(redqtsycode,code, reddjje);
                        int redid = Integer.valueOf(orderMapper.getMaxidByQTYS());
                        orderMapper.addQTYFdetailByStr(redid+"",reddjje);
                        //插入 应收应付余额明细表
                        orderMapper.addYSWLByQTYFCode(redqtsycode);
                    }
                    //再生成蓝字的QTYS
                    String qtsycode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                    //增加其他应收的定金
                    if(sadjje != null && !"".equals(sadjje) && Float.valueOf(sadjje) != 0){
                        orderMapper.addQTYFByPUStr(qtsycode,code,sadjje);
                        int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                        orderMapper.addQTYFdetailByStr(id+"",sadjje);
                        //插入 应收应付余额明细表
                        orderMapper.addYSWLByQTYFCode(qtsycode);
                    }
                }
            }else{
                if(params.get("yn")!=null && "是".equals(params.get("yn").toString())){
                    //如果没有来源单号，直接生成 QTYS
                    String qtyscode = "QTYF-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                    String djje = params.get("pubuserdefdecm1").toString();
                    //增加其他应收的定金
                    if(djje != null && !"".equals(djje) && Float.valueOf(djje) != 0){
                        orderMapper.addQTYFByPUStr(qtyscode,code,djje);
                        int id = Integer.valueOf(orderMapper.getMaxidByQTYS());
                        orderMapper.addQTYFdetailByStr(id+"",djje);
                        //插入 应收应付余额明细表
                        orderMapper.addYSWLByQTYFCode(qtyscode);
                    }
                }
            }
        }
    }

    @Override
    public void deleteQTYSByCode(String code) {
        synchronized (this){
            orderMapper.deleteQTYSdetail(code);
            orderMapper.deleteQTYS(code);
        }
    }

}