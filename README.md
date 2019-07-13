# 单片机固件烧录器

## 硬件需求与连接

  - 安卓6.0及以上
  
  - OTG转接线
  
  - CH340 USB to TTL
  
  - 你想要烧录的单片机
  
## 使用步骤

  - 选择hex文件
  
  - 选择单片机片内Flash大小
  
  - 开始烧录！！
  
  
## 注意事项
  
  当前暂只支持NXP的LPC8xx系单片机，后续会添加STM32的支持，最终希望实现大部分Cortex-M系的单片机烧录。
  
  选择Flash时需与实际Flash大小对应，否则有可能造成Flash的擦除和写入失败。
  
  
  
#### 联系方式 WeChat 
    xmh849873336
    
    
### 感谢以下开源库

https://github.com/felHR85/UsbSerial

https://github.com/leonHua/LFilePicker

https://blog.csdn.net/zf_c_cqupt/article/details/52676716 CSDN
