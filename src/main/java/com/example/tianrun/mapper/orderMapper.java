package com.example.tianrun.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface orderMapper {

    //返回数据库中 XXX_code_token 所有记录的 企业信息
    List<Map<String,String>> getDBAllOrgList();

    //调用refreshtoken 后更新数据库
    void updateOrgToken(Map<String,String> updateMap);


    //根据appkey 获取 当前的 token
    String getTokenByAppKey(@Param("AppKey") String AppKey);

    //根据 OrgId 获取 当前的 AppKey 和 AppSecret
    Map<String,String> getAppKeySecretByAppKey(@Param("OrgId") String OrgId);

    Map<String,Object>  getXsddmapByCode(@Param("xsddcode") String xsddcode);

    String getTpartencodeByByJX(String partnerjx);//根据 供应商简写 返回 供应商编码（T+有简写）

    String getTpartencodeByByConCode(String pucode);//根据 excel 采购合同号查询对应的供应商编号

    String getTCustmorcodeByByJX(String custmorjx);//根据 客户简写 返回 客户编码（T+有简写）

    Map<String,Object> getTinventorycodeByJX(@Param("xm") String xm,@Param("pakge") String pakge,@Param("inventory") String inventory);//根据 存货名称 返回 存货编码

    String getSASourceVoucherDetailId(String contractcode,String tinventorycode);

    Map<String,Object> getPUSourceVoucherDetailId(String contractcode,String tinventorycode);

    void updatePUdetailBySTCode(String vourcherCode);

    void updateSAdetailBySTCode(String vourcherCode);

    void updateSASAPreReceiveAmount(String vourcherCode);

    void updateSAPreReceiveAmount(String vourcherCode);

    String getTaxByInventoryCode(String inventorycode);

    List<Map<String,Object>> getCustmoryushouByCode(String code);

    void updateSAYuShou(Map<String,Object> custmoryushou);

    void addSAYuShou(Map<String,Object> custmoryushou);

    void addSAYuShouList(List<Map<String,Object>> custmoryushoulist);

    void updateSaDetailBySaOrderCode(String code);

    void updateSaDeliveryDetailBySaOrderCode(String code);

    String getTotalYushouAmountByCode(String code);

    Map<String,Object> getCustmorRule3Byname(String name);

    String getYushouTypeByCode(String code);

    String getRealYushouTypeByTcustmorname(String name);
}
