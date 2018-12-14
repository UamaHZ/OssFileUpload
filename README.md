# OSS项目封装

##项目Target

- [x] `支持多文件同步上传`
- [x] `支持多队列上传`
- [x] `支持文件上传运行时缓存策略`
- [x] `支持随时取消上传`
- [x] `支持配置单次最大上传数（不大于9）`
- [ ] `待支持—配置工厂模式支持多种上传策略`

## 集成

**在项目入口，如Application中，进行数据初始化**

1. 创建`OSSCredentialProvider`对象：Oss提供两种方案，`STS鉴权模式` 和 `自签名模式` ，参考链接[访问控制](https://help.aliyun.com/document_detail/32046.html?spm=a2c4g.11186623.6.953.68983915Nyb4wv)

   - `自签名模式`如下创建`OSSCredentialProvider`

     ```java
     OSSCredentialProvider credentialProvider = new OSSCustomSignerCredentialProvider() {
     	@Override
     	public String signContent(String content) {
     		// 您需要在这里依照OSS规定的签名算法，实现加签一串字符内容，并把得到的签名传拼接上AccessKeyId后返回
         	// 一般实现是，将字符内容post到您的业务服务器，然后返回签名
         	// 如果因为某种原因加签失败，描述error信息后，返回nil
         	
         	// 以下是用本地算法进行的演示
         	return "OSS " + AccessKeyId + ":" + base64(hmac-sha1(AccessKeySecret, content));
     	}
     };
     ```

     

   - `STS鉴权模式`如下创建`OSSCredentialProvider`

   ```java
   OSSCredentialProvider credentialProvider = new OSSAuthCredentialsProvider(OssStsServer);
   ```

2. 调用`OssProvider`提供的`init`函数，将`Oss初始化`必须的`OssBucketName`，上一步创建的`OSSCredentialProvider`对象，`application`以及`ossEndPoint`对象传入。

   ```java
    OssProvider.getInstance().init(ossBucketName,credentialProvider,application,ossEndPoint)
   ```

3. 项目支持自定义`链接超时时间（默认15秒）`，`最大并发请求数（默认9）`，`失败后重试次数（默认2）`

   ```java
   public static int OssConnectTime=15 * 1000;// 连接超时，默认15秒
       public static int OssSocketTime=15 * 1000; // socket超时，默认15秒
       public static int MaxUploadNumber=9; // 最大并发请求数，默认9个;最大并发图片请求数；
       public static int MaxErrorRetry=2; // 失败后最大重试次数，默认2次
   ```

4. 支持配置日志文件开启,默认关闭

   ```java
    OssUploadLog.enableLog()
   ```

   

## 使用

1. `上传` ，用`RealOssUpload`   来 实例化一个`IOssUpload`接口，调用`upLoad`方法，在回调中监听上传结果

   ```kotlin
   RealOssUpload().upLoad("user", pathList,object : UploadListener{
               override fun onError(msg: String) {
                   tx.text = msg
               }
   
               override fun onSuccess(mutableList: MutableList<String>) {
                   val a =  mutableList.joinToString ("\n")
                   OssUploadLog.i(TAG,"result-->$a")
                   tx.text = a
                   OssUploadLog.i(TAG,"tx_result.text -->"+tx.text )
               }
           })
   ```

2. `取消`，在页面销毁或者想手动取消时，调用`cancel`方法

   ```java
   upload.cancelUpload()
   ```

   

