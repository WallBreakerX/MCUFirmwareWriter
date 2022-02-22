# 单片机固件烧录器-安卓 MCU Firmware Writer for Android

## 注意事项
  
  - 当前暂只支持 **NXP的LPC8xx系芯片**，故以下引脚连接方式暂只适用于此系芯片。

  - 选择Flash时需与实际Flash大小对应(默认为16KB)，否则有可能造成Flash的擦除和写入失败。
  
  - 连接RTS与DTS引脚是为了让APP控制引脚电平使芯片进入ISP模式，也可自己手动进入ISP。

## 硬件需求与连接

  - 安卓6.0手机及以上
  
  - OTG转接线
  
  - CH340 USB to TTL
  
  - 你想要烧录的单片机
  
  - 将CH340的USB端转接至手机USB口
  
    TTL端的**TX 连接至 PIO0_0， RX 连接至 PIO0_4**
  
    **RTS 连接至 RESET，DTS 连接至 PIO0_12**
    
    (大部分CH340有引出RTS和DTS，但只有孔位，需要自行焊接针脚)
    
    VCC和GND连接好即可 (供电一定要另接一个，手机USB口电压不足，否则电平无法改变)
  
## 使用步骤

  - 选择hex文件
  
  - 选择片内Flash大小
  
  - 开始烧录！
  
  
### 感谢以下开源库

- https://github.com/felHR85/UsbSerial

- https://github.com/leonHua/LFilePicker

- https://blog.csdn.net/zf_c_cqupt/article/details/52676716 CSDN

### -

离开程序员行业了，有缘再见。
