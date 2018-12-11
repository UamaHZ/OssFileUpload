package com.uama.oss

import android.app.Application
import android.text.TextUtils
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.OSS
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider

class OssProvider:IOssProvider{
    companion object {
        var mApplication: Application? =null
        var mOssCredentialProvider: OSSCredentialProvider?=null
        var OssBucketName:String?=null
        var OssEndPoint:String?=null

        /**
         * OssProvider是单例的：建议于application 初始化时调用
         */
        fun getInstance():OssProvider{
            return OssProvider()
        }
    }
    /**
     * 注意：此方法必须要在使用oss上传前初始化；
     * @param ossCredentialProvider 有两个实现方式：STS授权 or 本地配置参数，参考注释下的例子使用
     * OSSCredentialProvider credentialProvider = new OSSAuthCredentialsProvider(OssStsServer);
     *
        OSSCredentialProvider credentialProvider =  new OSSCustomSignerCredentialProvider() {
            @Override
            public String signContent(String content) {
            // 您需要在这里依照OSS规定的签名算法，实现加签一串字符内容，并把得到的签名传拼接上AccessKeyId后返回
            // 一般实现是，将字符内容post到您的业务服务器，然后返回签名
            // 如果因为某种原因加签失败，描述error信息后，返回nil
            // 以下是用本地算法进行的演示
            return  OSSUtils.sign("LTAI7UitLPvagGI8","ACw3NJz3C3Up0y1x0doqz4KF3vsZRT",content);
            }
        };
     */
    fun init(ossBucketName:String,ossCredentialProvider: OSSCredentialProvider,application:Application,ossEndPoint:String? = OssConfig.DefaultOssEndPoint){
        OssBucketName = ossBucketName
        mOssCredentialProvider = ossCredentialProvider
        mApplication = application
        if(!TextUtils.isEmpty(ossEndPoint)){
            OssEndPoint = ossEndPoint
        }
    }

    override fun providerOss(): OSS {
        if(TextUtils.isEmpty(OssBucketName)){
            throw NullPointerException("you need initialize OssBucketName")
        }
        if(mApplication == null){
            throw IllegalStateException("you need initialize application")
        }
        when(mOssCredentialProvider == null ){
            true->throw IllegalStateException("Don't initialize ossCredentialProvider;you need to call init() at first")
            false->{
                val conf = ClientConfiguration()
                conf.connectionTimeout = OssConfig.OssConnectTime // 连接超时，默认15秒
                conf.socketTimeout = OssConfig.OssSocketTime // socket超时，默认15秒
                conf.maxConcurrentRequest = OssConfig.MaxUploadNumber // 最大并发请求数，默认9个
                conf.maxErrorRetry = OssConfig.MaxErrorRetry // 失败后最大重试次数，默认2次
                return OSSClient(mApplication, OssEndPoint, mOssCredentialProvider)
            }
        }
    }
}
