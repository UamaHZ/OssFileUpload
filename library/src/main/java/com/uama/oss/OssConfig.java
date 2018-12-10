package com.uama.oss;

/**
 * Created by guozhen.hou on 2018/7/23.
 * oss服务器配置信息
 * @link https://help.aliyun.com/document_detail/31837.html?spm=a2c4g.11186623.2.8.68352d71hgc9Y9#concept-zt4-cvy-5db
 * @link https://help.aliyun.com/document_detail/31837.html?spm=a2c4g.11186623.2.10.68352d71hgc9Y9#concept-zt4-cvy-5db
 */

public class OssConfig {
    public static String DefaultOssEndPoint="http://oss-cn-hangzhou.aliyuncs.com";//oss地域地址

    public static int OssConnectTime=15 * 1000;// 连接超时，默认15秒
    public static int OssSocketTime=15 * 1000; // socket超时，默认15秒
    public static int MaxUploadNumber=9; // 最大并发请求数，默认9个;最大并发图片请求数；
    public static int MaxErrorRetry=2; // 失败后最大重试次数，默认2次
}
