package com.example.tianrun.service;

import java.util.List;

public interface BasicService {

    String getResultByExcelList(List<Object>  list);

    String getResultBySaParams(String code,String xsddcode,String codeday,String numbers,String totalamount,String customername,String skcode);

}