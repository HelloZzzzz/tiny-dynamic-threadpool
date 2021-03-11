### tiny-dynamic-threadpool

### 关于
参考美团技术团队文章中的线程池在业务中的实践有感，通过长轮询简单实现的一个简单动态线程池demo

客户端访问服务端获取参数是否有改变，当参数有改变时，服务端长轮询推送到客户端，此时服务端获取参数动态的修改线程池参数

目前只实现了对指定线程池名称修改corePoolSize和maximumPoolSize

通过访问以下链接实现动态修改参数

http://127.0.0.1:8080/publishConfig?threadPoolName=busy&corePoolSize=2&maximumPoolSize=6

http://127.0.0.1:8080/publishConfig?threadPoolName=empty&corePoolSize=6&maximumPoolSize=6


参考文章：

https://mp.weixin.qq.com/s?__biz=MjM5NjQ5MTI5OA==&mid=2651751537&idx=1&sn=c50a434302cc06797828782970da190e&scene=21



