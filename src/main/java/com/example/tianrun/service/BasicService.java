package com.example.tianrun.service;

import java.util.List;

public interface BasicService {

    String getResultByExcelList(List<Object>  list);

    //String getResultBySaParams(String code,String xsddcode,String codeday,String numbers,String totalamount,String customername,String skcode);

    String getResultBySaParams(String code);

    String getResultByPUParams(String code);

    String auqtysByCode(String code,String djje,String yn,String type);

    String auqtyfByCode(String code,String djje,String yn,String type);

    void dealQTYSBySaOrderCode(String code);

    void dealQTYFBySaOrderCode(String code);

    void deleteQTYSByCode(String code);

}