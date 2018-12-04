package com.uama.oss;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by guozhen.hou on 2018/7/23.
 * oss服务器配置信息
 * @link https://help.aliyun.com/document_detail/31837.html?spm=a2c4g.11186623.2.8.68352d71hgc9Y9#concept-zt4-cvy-5db
 * @link https://help.aliyun.com/document_detail/31837.html?spm=a2c4g.11186623.2.10.68352d71hgc9Y9#concept-zt4-cvy-5db
 */

public class OssConfig {
    public static String OssEndPoint="http://oss-cn-hangzhou.aliyuncs.com";//oss地域地址

    private static String OssStsServer = "";//sts服务器，安全性比较高
    public static int OssConnectTime=15 * 1000;// 连接超时，默认15秒
    public static int OssSocketTime=15 * 1000; // socket超时，默认15秒
    public static int MaxUploadNumber=9; // 最大并发请求数，默认9个;最大并发图片请求数；
    public static int MaxErrorRetry=2; // 失败后最大重试次数，默认2次

    static String OssBucketName = "huilaila-pub"; //https://help.aliyun.com/document_detail/31837.html?spm=a2c4g.11186623.2.10.68352d71hgc9Y9#concept-zt4-cvy-5db

   public static void setOssSts(String oss){
        if(!TextUtils.isEmpty(oss)) OssStsServer=oss;
   }

   public static String getOssStsServer(){
       return OssStsServer;
   }

    /**
     * 此处创建一个文件上传缓存类
     * isSuccess：表示上传结果
     * netUrl:表示上传路径
     * @note 如果在app启动过程中，上传文件在oss后台被删除，app依旧会记录为已上传，此时会出现上传错误路径的情况
     */
   public static class UploadResult{
       private boolean isSuccess;
       private String netUrl;

       public UploadResult(boolean isSuccess,String netUrl){
           this.isSuccess = isSuccess;
           this.netUrl = netUrl;
       }
   }
}
