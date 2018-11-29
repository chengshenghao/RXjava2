# Jenkins使用

[参考链接](https://blog.csdn.net/zhuyb829/article/details/78899465)
### 1.环境检查
- 1 、Java环境变量配置检查：打开cmd，输入java -version，可以看到打印的Java版本信息。如果没有，请配置java环境变量。
- 2.Gradle环境变量配置检查：打开cmd，输入gradle -v，可以看到打印的Gradle版本信息。如果没有，请配置Gradle环境变量。
###2.Jenkins配置
- 1.放置war包（优先考虑此种方式，不易使用安装jenkins的方式，由于bug较多）
将下载的Jenkins war包放到Tomcat的webapps目录下，比如我的是F:\DE\apache-tomcat-7.0.67\webapps
- 2启动tomcat
- 3登录初始化密码（按提示路径打开密码文件，输入密码）、设置用户密码、安装插件等
- 4(参考图片0-4)
