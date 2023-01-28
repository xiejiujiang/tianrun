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
            for(int i=0;i<list.size();i++){
                Object oo = list.get(i);
                RetailTianrun retailTianrun = (RetailTianrun)oo;
                String sacode = "SA-"  + new SimpleDateFormat("yyyyMMdd").format(new Date()) + Md5.md5(""+Math.random()).substring(0,5);
                retailTianrun.setSacode(sacode);

                sadatalist.add(retailTianrun);
                datalist.add(retailTianrun);

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
                //先处理部分数据！
                for (int i = 0;i<datalist.size();i++){//同时 处理了  进货list  和   销货List
                    RetailTianrun retailTianrun = datalist.get(i);
                    retailTianrun.setTpartencode(orderMapper.getTpartencodeByByConCode(retailTianrun.getContractcode()));//根据采购合同号查询对应的供应商编号
                    sadatalist.get(i).setTcustmorcode(orderMapper.getTCustmorcodeByByJX(retailTianrun.getGetcustomer()));
                    //交货地点——》油厂（决定是否扣包装）；包装（KG》包粕：散装》散粕） + 物料名称 ——》 这三样 一共决定  存货 以及 对应的 主单位(顺带取出 项目编号——》就是油厂名称对应的编号)
                    Map<String,Object> tinventorycode = orderMapper.getTinventorycodeByJX(retailTianrun.getDeliveryplace(),"%"+retailTianrun.getPacking()+"%",retailTianrun.getInventory());
                    retailTianrun.setTinventorycode(tinventorycode.get("code").toString());
                    sadatalist.get(i).setTinventorycode(tinventorycode.get("code").toString());
                    // 销售不再关联 来源 单号
                    //String sasourceVoucherDetailId = orderMapper.getSASourceVoucherDetailId(retailTianrun.getContractcode(),tinventorycode);
                    //retailTianrun.setSasourceVoucherDetailId(sasourceVoucherDetailId);//查询出来源单据的明细ID

                    //进货明细 的 来源单据单据对应的明细行ID  以及  订单上 这一行的 蛋白差！
                    Map<String,Object> pusourceVoucherDetailMap = orderMapper.getPUSourceVoucherDetailId(retailTianrun.getContractcode(),tinventorycode.get("code").toString());
                    if(pusourceVoucherDetailMap!=null){
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
                        return "未找到对于的采购订单（合同号），请先检查单据！";
                    }
                }

                String token = orderMapper.getTokenByAppKey("iiQG1E7l");// 天润的appkey
                //根据 excel 的内容，自动生成 进货单，都是 保存状态 ！
                List<String> pujsons = ListToJson.getPuJsonByList(datalist);
                String pufailstr = "";
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
                                pufailstr = pufailstr + "," + (i+2) ;
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

                //------------------------------------------------------------------------------------------------//
                //根据 excel 的内容，自动生成 销货单 ，都是 保存状态 ！
                List<String> sajsons = ListToJson.getSaJsonByList(sadatalist);
                String safailstr = "";
                for(int i=0;i<sajsons.size();i++){
                    String sacode = sadatalist.get(i).getSacode();
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
                                RetailTianrun retailTianrun = sadatalist.get(i);//拿到对应的 原始参数
                                String tcustmorcode = retailTianrun.getTcustmorcode();//客户编号
                                List<Map<String,Object>> custmoryushoulist = orderMapper.getCustmoryushouByCode(tcustmorcode);
                                if(custmoryushoulist != null && custmoryushoulist.size() == 1
                                        && custmoryushoulist.get(0).get("code") != null
                                        && !"NULL".equals(custmoryushoulist.get(0).get("code").toString())){
                                    //只有一个预收款
                                    Map<String,Object> custmoryushou = custmoryushoulist.get(0);
                                    Float amount = Float.valueOf(custmoryushou.get("amount").toString());
                                    if(amount >= (Float.valueOf(retailTianrun.getPlansalenumbers()) * Float.valueOf(retailTianrun.getTaxprice()))){
                                        custmoryushou.put("cancelamount",Float.valueOf(retailTianrun.getPlansalenumbers()) * Float.valueOf(retailTianrun.getTaxprice()));
                                        custmoryushou.put("sacode",sacode);
                                        orderMapper.updateSAYuShou(custmoryushou);
                                        orderMapper.addSAYuShou(custmoryushou);// 这个 日期 要 改成 预收款的 日期
                                    }
                                }
                                if(custmoryushoulist != null && custmoryushoulist.size() > 1
                                        && custmoryushoulist.get(0).get("code") != null
                                        && !"NULL".equals(custmoryushoulist.get(0).get("code").toString())){
                                    //有多个预收款!
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
                                }
                            }else{
                                safailstr = safailstr + "," + (i+2) ;
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                if("".equals(pufailstr) && "".equals(safailstr)){
                    return "全部成功，没有失败！";
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

    @Override
    public String getResultBySaParams(String code, String xsddcode, String codeday, String numbers, String totalamount, String customername,String skcodes) {
        Map<String,Object> result = new HashMap<String,Object>();
        try {
            // 如果使用了 定金 ,合同执行金额
            // skcode : ，",213e23ede,12e3dqd243,12243d4d"
            if(skcodes != null && !"".equals(skcodes)){
                String yushoutypes = "";
                for(String skcode : skcodes.split(",")){
                    if(skcode != null && !"".equals(skcode)){
                        String yushoutype = orderMapper.getYushouTypeByCode(skcode);
                        yushoutypes = yushoutypes + ","+yushoutype;
                    }
                }
                String yushoutotal = orderMapper.getRealYushouTypeByTcustmorname(customername);
                if(Float.valueOf(yushoutotal) >= Float.valueOf(totalamount) && yushoutypes.contains("定金")){
                    result.put("code","9999");
                    result.put("msg","此客户预收款足够，不能使用定金！");
                    JSONObject job = new JSONObject(result);
                    return job.toJSONString();
                }
            }

            Map<String,Object> rule3 = orderMapper.getCustmorRule3Byname(customername);
            if("先款".equals(rule3.get("sktype").toString()) && Float.valueOf(rule3.get("totalamount").toString()) < Float.valueOf(totalamount) ){
                result.put("code","9999");
                result.put("msg","此客户是先款客户，并且 预收款不够用了！");
                JSONObject job = new JSONObject(result);
                return job.toJSONString();
            }
            //查询这个销售订单 后续 所有销货单（保存/审核中/审核）的数量总数，以及 这个 订单对应的 表头上的 开始日期-结束日期
            //表头上 这个地方  选单之后 是空白的！！！
            Map<String,Object> xsddmap = orderMapper.getXsddmapByCode(xsddcode);
            if( xsddmap != null  &&  xsddmap.get("startdate") != null ){
                String totalNumbers = xsddmap.get("totalNumbers").toString();//这个销售订单后续所有销货单的总数量
                String ddNumbers = xsddmap.get("ddNumbers").toString();// 销售订单 本身的总数量
                String starteDate = xsddmap.get("startdate").toString();// 销售订单 明细里面的开始时间
                String enddate = xsddmap.get("enddate").toString();//  销售订单 明细里面的结束时间
                if(DateUtil.isEffectiveDate(new SimpleDateFormat().parse(codeday),
                        new SimpleDateFormat().parse(starteDate),
                        new SimpleDateFormat().parse(enddate))
                        && (Float.valueOf(numbers) + Float.valueOf(totalNumbers)) <= Float.valueOf(ddNumbers) ){
                    result.put("code","0000");
                    result.put("msg","允许保存");
                    JSONObject job = new JSONObject(result);
                    return job.toJSONString();
                }else{
                    result.put("code","9999");
                    result.put("msg","超出订单执行数量 或者 时间 ，不允许保存！");
                    JSONObject job = new JSONObject(result);
                    return job.toJSONString();
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
}