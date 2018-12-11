OSS项目封装

Target

- 支持多文件同步上传
- 支持多队列上传
- 支持文件上传运行时缓存策略
- 支持随时取消上传
- 支持配置单次最大上传数（不大于9）
- 待支持—配置工厂模式支持多种上传策略

集成

在项目入口，如Application中，进行数据初始化

1. 创建OSSCredentialProvider对象：Oss提供两种方案，STS鉴权模式 和 自签名模式 ，参考链接访问控制
2. 调用OssProvider提供的init函数，将Oss初始化必须的OssBucketName，上一步创建的OSSCredentialProvider对象，application以及ossEndPoint对象传入。
3. 项目支持自定义链接超时时间（默认15秒），最大并发请求数（默认9），失败后重试次数（默认2）

使用

1. 上传 ，用RealOssUpload   来 实例化一个IOssUpload接口，调用upLoad方法，在回调中监听上传结果
2. 取消，在页面销毁或者想手动取消时，调用cancel方法


