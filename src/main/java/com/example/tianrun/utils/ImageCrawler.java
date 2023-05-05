package com.example.tianrun.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLSocketFactory;

public class ImageCrawler {

    public static void main(String[] args) {
        try {
            downloadWoman();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadWoman() throws Exception {
        String url = "https://www.umei.cc/meinvtupian/xingganmeinv/";
        Document document = Jsoup.parse(new URL(url), 10000);
        Element content = document.getElementById("infinite_scroll");
        //for(Element content:contents){
            Elements liElements = content.getElementsByClass("img");
            for (Element liElement : liElements) {
                Elements img = liElement.getElementsByTag("img");
                System.out.println("img ===== " + img);
                String src = img.attr("data-original");
                System.out.println("src ===== " + src);
                if(src.contains("jpg")){
                    String pictureName = img.attr("alt");
                    URL target = new URL(src);
                    InputStream is = target.openConnection().getInputStream();
                    FileOutputStream fos = new FileOutputStream("E:\\imagesWork\\" + pictureName + ".jpg");
                    int len = 0;
                    byte[] bytes = new byte[1024];
                    while ((len = is.read(bytes)) != -1) {
                        fos.write(bytes, 0, len);
                    }
                    System.out.println(pictureName + "--->下载完成");
                    fos.close();
                    is.close();
                }
            }
        //}
    }
}