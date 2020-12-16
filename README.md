# RabbitBot 兔叽
-----
原本的项目依赖酷q，很可惜酷q莫得了，但有幸遇到mirai，兔叽还有复活的机会(°∀°)ﾉ

RabbitBot_RE是基于java语言和mirai框架的群聊机器人<br/>
程序建立于 spring + maven 的基础上<br/>

    大部分回复并非一成不变，采用随机回复，让兔子看起来不那么死板

### 程序包含:
* mirai 
* maven 用于管理项目
* spring  用于...其实我是馋spring的自动注入，可太爽了
* quartz  用于定时任务
* lombok  用于简化代码
* junit 用于单元测试
* logback 用于输出日志，以及日志数据持久化
* 还有兔子 \兔子万岁/

<br/><br/>
### 功能
* 指令模式代码构造
* 远程热更新业务配置参数
* 微博消息推送
* 根据图片id，tag，作者名称，获取pixiv图片
* 获取pixiv日榜
* Saucenao搜图
* 群复读(添加了复读概率)
* 关键词回复
* 问候反馈
* 扭蛋功能
* 随机高强度密码
* 当前天气状况
* 整点报时
* 摩斯电码转化
* 生成二维码

<br/><br/>
|指令"()"为必填参数"[]"为可选参数|详情|
|----|-----|
|.help|帮助列表，不过不完善，毕竟把东西都写上去太臃肿了|
|.r|生成一个1-100的随机数|
|.rpwd [长度]|随机一个(看起来)强度很高的密码，默认长度为6|
|.扭蛋|随机抽取一个神奇的东西|
|.pid (pixivImgId)|根据pixiv图片id搜索图片|
|.ptag (pixivImgTag)|根据pixiv图片tag搜索图片，结果是随机的，但分数越高的作品权重越高|
|.搜图 (图片)|接入了Saucenao的搜图功能|
|.来点色图|随机出现一张涩图，图片都源于pixiv|
|.say|随|

