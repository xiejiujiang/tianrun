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

    Map<String,Object>  getXsddmapByCode(String xsddcode);

    Map<String,Object>  getCgddmapByCode(String cgddcode);

    String getTpartencodeByByJX(String partnerjx);//根据 供应商简写 返回 供应商编码（T+有简写）

    String getTpartencodeByByConCode(String pucode);//根据 excel 采购合同号查询对应的供应商编号

    Map<String,Object> getTCustmorcodeByByJX(String custmorjx);//根据 客户简写 返回 客户编码（T+有简写）  和 对应结算客户的code

    Map<String,Object> getTinventorycodeByJX(String name,String productInfo,String xm);//根据 存货名称 返回 存货编码

    String getSASourceVoucherDetailId(String contractcode,String tinventorycode);

    Map<String,Object> getPUSourceVoucherDetailId(String contractcode,String tinventorycode);

    void updatePUdetailBySTCode(String vourcherCode);

    void updateSAdetailBySTCode(String vourcherCode);

    void updateARAPDetailBySABusinessCode(String vourcherCode);

    void updateARAPDetailByPUBusinessCode(String vourcherCode);

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

    String getXMkoudaiByName(String name);

    void addQTYSByStr(String qtyscode,String code,String djje);//通过 报价单 生成

    void addQTYFByStr(String qtyfcode,String code,String djje);//通过 请购单 生成

    void addQTYSBySAStr(String qtyscode,String code,String djje);//通过 销售订单 生成

    void addREDQTYSBySAStr(String qtyscode,String code,String djje);//通过 销售订单 生成

    void addQTYFByPUStr(String qtyscode,String code,String djje);//通过 采购订单 生成

    void addREDQTYFByPUStr(String qtyscode,String code,String djje);//通过 采购订单 生成

    Integer getMaxidByQTYS();

    Integer getMaxidByQTYF();

    void addQTYSdetailByStr(String id,String djje,String descc);

    void addQTYFdetailByStr(String id,String djje,String descc);

    void deleteQTYSdetail(String code);

    void deleteQTYS(String code);

    Map<String,Object> getSaorderDetailByCode(String code);

    Map<String,Object> getPuorderDetailByCode(String code);

    String getQTSYcanuseByCode(String xsddcode);

    String getQTYFcanuseByCode(String code);

    String  getddNumbersByCode(String xsddcode);

    String getcgNumbersByCode(String code);

    void addYSWLByQTYSCode(String qtsycode);

    void addYSWLByQTYFCode(String qtyfcode);

    void deleteYSWLByQTYSCode(String qtsycode);

    List<Map<String,Object>> getSadetailByCode(String code);

    List<Map<String,Object>> getPudetailByCode(String code);

    String getPuNumbersByCode(String code);

    String getSaNumbersByCode(String code);

    Integer getRecordQTYSByCode(String bjcode,String code);

    Integer getBJQTYSByCode(String code);

    Integer getQGQTYFByCode(String code);

    Integer getRecordQTYSByDDCode(String code);

    Integer getRecordQTYFByCode(String qgcode,String code);

    Integer getRecordQTYFByDDCode(String code);

    void updateQTYSdetailByStr(String code,String djje);

    void updateRedQTYSdetailByStr(String bjcode,String code,String djje);

    void updateYSWLByQTYSCode(String code,String djje);

    void updateRedYSWLByQTYSCode(String bjcode,String code,String djje);

    void updateQTYFdetailByStr(String code,String djje);

    void updateRedQTYFdetailByStr(String qgcode,String code,String djje);

    void updateYSWLByQTYFCode(String code,String djje);

    void updateRedYSWLByQTYFCode(String qgcode,String code,String djje);

    void updatePuCreatorAndCleakByID(String id);

    void updateSaCreatorAndClerkByCode(String code);

    void updatePurchaseRequisitionCX(String code);

    void updateSaleQuotationCX(String code);

    void updateSaorderCX(String xsddcode);

    void updatePuorderCX(String cgddcode);

    void updateSaleDeliverySourceRelation(String code);

    void updatePurchaseArrivalSourceRelation(String code);

    void updateSaorderBySTInventoryQuanity(String code);

    void updatePuorderBySTInventoryQuanity(String code);
}
