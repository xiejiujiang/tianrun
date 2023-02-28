package com.example.tianrun.controller;

import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.example.tianrun.SAsubscribe.SACsubJsonRootBean;
import com.example.tianrun.entity.RetailTianrun;
import com.example.tianrun.service.BasicService;
import com.example.tianrun.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import com.example.tianrun.mapper.orderMapper;
import java.util.*;

@CrossOrigin
@Controller
@RequestMapping(value = "/token")
public class TokenController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenController.class);

    @Autowired
    private BasicService basicService;

    @Autowired
    private orderMapper orderMapper;

    //这个里面 主要 用来 接受 code ,刷新 token ，更新对应的数据库
    @RequestMapping(value="/recode", method = {RequestMethod.GET,RequestMethod.POST})
    public @ResponseBody String recode(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.info("------------------- 正式OAuth回调地址 -------------------");
        String code = request.getParameter("code");
        //第一次授权后，会有这个code,立刻调用 一次 授权码换token接口 ，拿到完整的 token 相关信息，并写入数据库。
        //3月17日思考： 暂时不用接口来访问，直接在线访问后 拿到第一次的数据，并 复制 填入数据库表中接口（后续定时任务来更新）
        return code;
    }


    //T+ 的 消息订阅的接口。
    @RequestMapping(value="/ticket", method = {RequestMethod.GET,RequestMethod.POST})
    public @ResponseBody String reticket(HttpServletRequest request, HttpServletResponse response) {
        try{
            InputStreamReader reader=new InputStreamReader(request.getInputStream(),"utf-8");
            BufferedReader buffer=new BufferedReader(reader);
            String params=buffer.readLine();
            JSONObject jsonObject = JSONObject.parseObject(params);
            String encryptMsg = jsonObject.getString("encryptMsg");
            String destr = AESUtils.aesDecrypt(encryptMsg,"123456789012345x");
            // {"id":"AC1C04B100013301500B4A9B012DB2EC","appKey":"A9A9WH1i","appId":"58","msgType":"SaleDelivery_Audit","time":"1649994072443","bizContent":{"externalCode":"","voucherID":"23","voucherDate":"2022/4/15 0:00:00","voucherCode":"SA-2022-04-0011"},"orgId":"90015999132","requestId":"86231b63-f0c2-4de1-86e9-70557ba9cd62"}
            JSONObject job = JSONObject.parseObject(destr);
            LOGGER.info("------------------- 正式消息接收地址，包含 ticket，消息订阅，具体是："+job.getString("msgType")+" -------------------");
            // 采购入库单审核 订阅
            if("PurchaseReceiveVoucher_Audit".equals(job.getString("msgType"))){
                SACsubJsonRootBean jrb =  job.toJavaObject(SACsubJsonRootBean.class);
                String vourcherCode = jrb.getBizContent().getVoucherCode();
                LOGGER.info("-------------------采购入库单：" + vourcherCode + "审核信息收到，马上进行处理-------------------");
                //查询下 这个 入库单上的 商品 和 对应的 数量，金额。以及 对应的 进货单是哪个，然后再更新进货单上面的内容
                orderMapper.updatePUdetailBySTCode(vourcherCode);
                //更新下进货单对应的 PU_PurchaseArrival_SourceRelation
                orderMapper.updatePurchaseArrivalSourceRelation(vourcherCode);
                //更新 应收应付明细账DTO（ARAP_Detail）中的对于金额
                orderMapper.updateARAPDetailByPUBusinessCode(vourcherCode);
            }

            // 销售出库单审核 订阅
            if("SaleDispatchVoucher_Audit".equals(job.getString("msgType"))){
                SACsubJsonRootBean jrb =  job.toJavaObject(SACsubJsonRootBean.class);
                String vourcherCode = jrb.getBizContent().getVoucherCode();
                LOGGER.info("-------------------销售出库单：" + vourcherCode + "审核信息收到，马上进行处理-------------------");
                // 审核之后 把 实际 数量 反写回 销货单的 数量上， 并自动计算 差异 ，写入 数量差异字段（都是是自定义字段）
                orderMapper.updateSAdetailBySTCode(vourcherCode);//还要更新下 差异字段哦
                //更新下对应的销货单上游 SA_SaleDeliverySourceRelation 里面的数量
                orderMapper.updateSaleDeliverySourceRelation(vourcherCode);
                //更新 应收应付明细账DTO（ARAP_Detail）中的对于金额
                orderMapper.updateARAPDetailBySABusinessCode(vourcherCode);

                // 也需要把  预收款的核销金额 同步 。
                //先更新 销货单上的 预收金额
                //orderMapper.updateSASAPreReceiveAmount(vourcherCode);//根据明细 含税金额的总和 更新 对应销货单的使用预收 以及 核销金额
                //再更新 销货单预收明细里面的核销金额
                //orderMapper.updateSAPreReceiveAmount(vourcherCode);//根据明细 含税金额的总和 更新 对应销货单的使用预收 以及 核销金额
            }

            // 销售订单（合同）变更——》更新 后，保存, 处理定金（其他应收单）
            if("SaleOrder_Change".equals(job.getString("msgType"))
                || "SaleOrder_Update".equals(job.getString("msgType"))){
                SACsubJsonRootBean jrb =  job.toJavaObject(SACsubJsonRootBean.class);
                String vourcherCode = jrb.getBizContent().getVoucherCode();
                // 1 根据这个 新的销售订单的明细 商品 和 价格，更新下 已经生成的销货单上的 商品 价格
                // (暂定价出货的问题 不这样处理了，我还没用想好怎么处理，所以就先取消这个！)
                // orderMapper.updateSaDetailBySaOrderCode(vourcherCode);

                //因为：销售订单的定金 也是 审核不生产，变更——》保存才生成！（必须是 某人，且，必须是 是！ ）
                //如果有来源单号（报价单单号）：先用报价单单号 生成 其他应收（红字），金额是：报价单上的定金金额*（销售订单数量/报价单数量）
                //再生成 一个 其他应收单（蓝字），金额是：销售订单上的 定金金额（表头上的）合同定金 + 是否生成  来 自动生成 其他应收的蓝字（合同定金 ）
                basicService.dealQTYSBySaOrderCode(vourcherCode);
            }

            // ------------------------------------------------------------------------------------------//
            // 取消了 采购订单  审核  或者  弃审 来 做的 2和3 （挨着的上面 2和 3 ）
            // ------------------------------------------------------------------------------------------//


            // 采购订单（合同）变更——》更新 后，保存, 处理 定金（其他应付的问题）
            if("PurchaseOrder_Change".equals(job.getString("msgType"))
                    || "PurchaseOrder_Update".equals(job.getString("msgType"))){
                SACsubJsonRootBean jrb =  job.toJavaObject(SACsubJsonRootBean.class);
                String vourcherCode = jrb.getBizContent().getVoucherCode();
                //采购订单的定金 也是 在这里 处理了！！！
                // 因为：采购订单的定金 也是 审核不生产，变更——》保存才生成！（必须是 某人，且，必须是 是！ ）
                // 如果有来源单号（请购单单号）：先用请购单号 生成 其他应付（红字），金额是：请购单上的定金金额*（采购订单数量/请购单数量）
                // 再生成 一个 其他应付单（蓝字），金额是：采购订单上的 定金金额（表头上的）合同定金 + 是否生成  来 自动生成 其他应付的蓝字（合同定金 ）
                basicService.dealQTYFByPuOrderCode(vourcherCode);
            }

            //销货单审核，用来处理 使用定金的问题，并且 无需返回前端，默认（大于订单数量-5就自动使用定金）
            if("SaleDelivery_Audit".equals(job.getString("msgType"))){
                SACsubJsonRootBean jrb =  job.toJavaObject(SACsubJsonRootBean.class);
                String code = jrb.getBizContent().getVoucherCode();//销货单 单号
                basicService.getResultBySaParams(code);
            }

            //进货单审核，用来处理 使用定金的问题，并且 无需返回前端，默认（大于订单数量-5就自动使用定金）
            if("PuArrival_Audit".equals(job.getString("msgType"))){
                SACsubJsonRootBean jrb =  job.toJavaObject(SACsubJsonRootBean.class);
                String code = jrb.getBizContent().getVoucherCode();//进货单 单号
                basicService.getResultByPUParams(code);
            }

            // 用来处理复制行的问题
            // 销货单保存（修改） 这个只是用来处理，销货单选单后，客户不想复制明细的个别字段内容，所以通过SQL复制一下
            if("SaleDelivery_Update".equals(job.getString("msgType"))
                    || "SaleDelivery_Change".equals(job.getString("msgType"))){
                SACsubJsonRootBean jrb =  job.toJavaObject(SACsubJsonRootBean.class);
                String vourcherCode = jrb.getBizContent().getVoucherCode();
                // 根据这个 新的销售订单的明细 商品 和 价格，更新下 已经生成的销货单上的 商品 价格
                orderMapper.updateSaDeliveryDetailBySaOrderCode(vourcherCode);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "{ \"result\":\"success\" }";
    }


    // ------------------------------------------------  以下是业务接口 -----------------------------------------------------//

    //打开 excel 导入页面
    @RequestMapping(value="/openExceldeal", method = {RequestMethod.GET,RequestMethod.POST})
    public ModelAndView openExceldeal(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView();
        LOGGER.info("-------------------  打开打开 excel 导入页面 事件  ----------------------");
        mav.setViewName("excels/openExceldeal");
        return mav;
    }

    // 导入excel , 解析出 表格中的某个sheet 某行开始的 某几列
    @RequestMapping(value="/autoexcelinfo", method = {RequestMethod.GET,RequestMethod.POST})
    public @ResponseBody String autoexcelinfo(@RequestParam(value = "file") MultipartFile file, HttpServletRequest request, HttpServletResponse response) {
        try{
            String user = request.getParameter("user");
            LOGGER.info(" user ================== " + user);
            InputStream inputStream = file.getInputStream();
            ExcelListener listener = new ExcelListener();
            ExcelReader excelReader = new ExcelReader(inputStream, ExcelTypeEnum.XLS, null, listener);

            com.alibaba.excel.metadata.Sheet sheet = new Sheet(2,1, RetailTianrun.class);
            excelReader.read(sheet);
            List<Object> list = listener.getDatas();//当前从上传的excel中获取的数据
            String resulst = basicService.getResultByExcelList(list);
            return resulst;
        }catch (Exception e){
            e.printStackTrace();
            return "参数错误，请重试！";
        }
    }


    // 前端 已 废弃！！！！！！！！！
    // 2023-02-09 这个方法要放弃，放到 消息订阅的 销货单 审核里面。 保存 前 页面提示没有意义了，我可以直接点审核 绕开保存！
    // 销货单 保存后 的时候
    // 1. 判断 导入油厂和选单油厂是否一致（前端已完成）
    // 2. 查询当前数量 和 订单已经执行的数量 是否超过 订单总数  已经  当前单据时间 是否在 订单的有效时间内
    // 3. 如果 客户的 自定义项 是 先款，还需与判断 还能使用的预收款金额 》=  此单的销售金额 。否则，不卖！
    // 4. 如果上面三点都没问题才允许保存！ （再 通过 保存的消息订阅 来更新明细里面的  司机 4 个信息）
    @RequestMapping(value="/getDistricntKC", method = {RequestMethod.GET,RequestMethod.POST})
    public @ResponseBody String getDistricntKC(HttpServletRequest request, HttpServletResponse response) throws Exception{
        Map<String,Object> result = new HashMap<String,Object>();
        /*String code = request.getParameter("code");//当前的 单据编号(销货单)
        String xsddcode = request.getParameter("xsddcode");//当前的单据 对应的  销售订单单据编号
        String codeday = request.getParameter("codeday");//当前的 单据日期
        String numbers = request.getParameter("numbers");//当前单据的总数量
        String totalamount = request.getParameter("totalamount");//当前单据的总金额(含税)
        String customername = request.getParameter("customername");//客户名称
        String skcode = request.getParameter("skcode");//,'xxxxx','xxxxxxx','xxxxxx'  使用预收的收款单单号
        String saresult = basicService.getResultBySaParams(code,xsddcode,codeday,numbers,totalamount,customername,skcode);*/
        return "";
    }


    //报价单 审核 或者 弃审后，根据 单据编号， 自动生成 / 删除 对应的 其他应收
    //报价单：是否生成定金 默认是 空的。在保存审核后 不自动生成
    //更改到  指定人（zt_user 对应的ID） 进行 变更，变更后 再保存 才可以 自动生成定金
    //http://47.92.249.206:9991/tianrun/token/auqtysByCode?code=20230202-12&djje=14875&yn=&type=add
    @RequestMapping(value="/auqtysByCode", method = {RequestMethod.GET,RequestMethod.POST})
    public @ResponseBody String auqtysByCode(HttpServletRequest request, HttpServletResponse response) throws Exception{
        String code = request.getParameter("code");
        String djje = request.getParameter("djje");
        String yn = request.getParameter("yn");
        String type = request.getParameter("type");
        synchronized (this){
            String saresult = basicService.auqtysByCode(code,djje,yn,type);
            return saresult;
        }
    }


    //请购单：是否生成定金 默认是 空的。在保存审核后 不自动生成
    //更改到  指定人（zt_user 对应的ID） 进行 变更，变更后 再保存 才可以 自动生成定金 ，其他应付单
    //http://47.92.249.206:9991/tianrun/token/auqtyfByCode?code=20230202-12&djje=14875&yn=&type=add
    @RequestMapping(value="/auqtyfByCode", method = {RequestMethod.GET,RequestMethod.POST})
    public @ResponseBody String auqtyfByCode(HttpServletRequest request, HttpServletResponse response) throws Exception{
        String code = request.getParameter("code");
        String djje = request.getParameter("djje");
        String yn = request.getParameter("yn");
        String type = request.getParameter("type");
        synchronized (this){
            String saresult = basicService.auqtyfByCode(code,djje,yn,type);
            return saresult;
        }
    }


    //采购入库单 保存之前，判断 总数量 和  来源单据的总数量的 差异 是不是在 +- 1之内。否则 前端提示 要走审批
    @RequestMapping(value="/getNumbersBycgrk", method = {RequestMethod.GET,RequestMethod.POST})
    public @ResponseBody String getNumbersBycgrk(HttpServletRequest request, HttpServletResponse response) throws Exception{
        String lydh = request.getParameter("lydh");//来源单号（进货单）
        String numbers = request.getParameter("numbers");//当前单据总数量
        String lydhNumbers = orderMapper.getPuNumbersByCode(lydh);
        Map<String,Object> result = new HashMap<String,Object>();
        if( Float.valueOf(numbers) >= (Float.valueOf(lydhNumbers)-1)  &&  Float.valueOf(numbers) <= (Float.valueOf(lydhNumbers)+1)){
            result.put("code","0000");
            result.put("msg","可以保存！");
            JSONObject job = new JSONObject(result);
            return job.toJSONString();
        }else{
            result.put("code","9999");
            result.put("msg","数量超过来源单据的正负1之外！请在备注中添加”超量“关键字的说明后，再保存，进入待审状态！");
            JSONObject job = new JSONObject(result);
            return job.toJSONString();
        }
    }

    //销售出库单 保存之前，判断 总数量 和  来源单据的总数量的 差异 是不是在 +- 1之内。否则 前端提示 要走审批
    @RequestMapping(value="/getNumbersByxsck", method = {RequestMethod.GET,RequestMethod.POST})
    public @ResponseBody String getNumbersByxsck(HttpServletRequest request, HttpServletResponse response) throws Exception{
        String lydh = request.getParameter("lydh");//来源单号（销货单）
        String numbers = request.getParameter("numbers");//当前单据总数量
        String lydhNumbers = orderMapper.getSaNumbersByCode(lydh);
        Map<String,Object> result = new HashMap<String,Object>();
        if(Float.valueOf(numbers) >= (Float.valueOf(lydhNumbers)-1)  &&  Float.valueOf(numbers) <= (Float.valueOf(lydhNumbers)+1)){
            result.put("code","0000");
            result.put("msg","可以保存！");
            JSONObject job = new JSONObject(result);
            return job.toJSONString();
        }else{
            result.put("code","9999");
            result.put("msg","数量超过来源单据的正负1之外！请在备注中添加”超量“关键字的说明后，再保存，进入待审状态！");
            JSONObject job = new JSONObject(result);
            return job.toJSONString();
        }
    }
}