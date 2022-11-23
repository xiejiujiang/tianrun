package com.example.tianrun.controller;

import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.example.tianrun.service.TokenService;
import com.example.tianrun.utils.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.tianrun.entity.RetailCode;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

@CrossOrigin
@Controller
@RequestMapping(value = "/token")
public class TokenController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenController.class);

    @Autowired
    private TokenService tokenService;

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
        LOGGER.info("------------------- 正式消息接收地址，包含 ticket，消息订阅，授权 -------------------");
        try{
            InputStreamReader reader=new InputStreamReader(request.getInputStream(),"utf-8");
            BufferedReader buffer=new BufferedReader(reader);
            String params=buffer.readLine();
            JSONObject jsonObject = JSONObject.parseObject(params);
            String encryptMsg = jsonObject.getString("encryptMsg");
            String destr = AESUtil.decrypt(encryptMsg,"123456789012345x");
            // {"id":"AC1C04B100013301500B4A9B012DB2EC","appKey":"A9A9WH1i","appId":"58","msgType":"SaleDelivery_Audit","time":"1649994072443","bizContent":{"externalCode":"","voucherID":"23","voucherDate":"2022/4/15 0:00:00","voucherCode":"SA-2022-04-0011"},"orgId":"90015999132","requestId":"86231b63-f0c2-4de1-86e9-70557ba9cd62"}
            JSONObject job = JSONObject.parseObject(destr);
            if("SaleDelivery_Audit".equals(job.getString("msgType"))){
                //SACsubJsonRootBean jrb =  job.toJavaObject(SACsubJsonRootBean.class);//销货单的订阅信息DTO
                // 处理 正常的 销货单审核 上传 ，图片上传 功能   和  单据不上传，只上传图片的功能。

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
            InputStream inputStream = file.getInputStream();
            ExcelListener listener = new ExcelListener();
            ExcelReader excelReader = new ExcelReader(inputStream, ExcelTypeEnum.XLS, null, listener);

            //已经做好的 普天T+名称匹配表是 从 第一个sheet 的 第一行 开始获取数据
            com.alibaba.excel.metadata.Sheet sheet = new Sheet(1,0, RetailCode.class);
            excelReader.read(sheet);
            List<Object> list = listener.getDatas();//当前从上传的excel中获取的数据
            List<String> codelist = new ArrayList<String>();//最终读取之后的数据
            for(Object oo : list){
                RetailCode retailCode = (RetailCode)oo;
                if(retailCode != null && !"".equals(retailCode.getCode())){
                    codelist.add(retailCode.getCode());
                }
            }
            // 业务处理逻辑
            if(codelist != null && codelist.size() != 0){

                return "";
            }else{
                return "参数错误，请重试！";
            }
        }catch (Exception e){
            e.printStackTrace();
            return "参数错误，请重试！";
        }
    }

}