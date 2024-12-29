package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;
import java.util.TimerTask;

public class SenderSlidingWindow extends SlidingWindow {
    private volatile UDT_Timer timer = null; // 定时器
    private TCP_Sender tcpSender = null; // 发送端
    private volatile int repAck = 0; // 重复确认次数


    // 构造函数
    public SenderSlidingWindow(TCP_Sender tcpSender) {
        super();
        this.tcpSender = tcpSender;
    }

    // 启动计时器
    void startTimer() {
        if (timer != null) {
            try {
                timer.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (base > finalSeq) { // 由于这里是递归的，所以要加上结束
            return;
        }
        timer = new UDT_Timer();
        timer.schedule( new TimerTask() {
            @Override
            public void run() {
                System.out.println("发生了超时重传，base" + base);
                // 超时重传
                tcpSender.udt_send(dataMap.get(base));
                startTimer();
            }
        }, 3000);
    }

    public boolean putPacket(TCP_PACKET packet){
        if (packet.getTcpH().getTh_seq() > base + windowSize * singlePacketSize) {
            return false;
        }
        dataMap.put(packet.getTcpH().getTh_seq(), packet);
        return true;
    }

    public boolean recvAck(int ack){
        if (ack >= base+windowSize*singlePacketSize) {
            return false;
        }

        if (base > finalSeq) {
            return false;
        }

        if (ack == base - singlePacketSize) { // 快速重传
            repAck++;
            if (repAck == 3) {
                System.out.println("发生了快速重传，base" + base);
                repAck = 0;
                tcpSender.udt_send(dataMap.get(base)); // 这里出现了错误，没有发base
                startTimer();
            }
        } else {
            slide(ack);
            startTimer();
        }
        return true;
    }

    void slide(int ack){
        System.out.println("接收到的ack是" + ack);
        for (; base <= ack; base += singlePacketSize) {
            System.out.println("发送方窗口开始滑动：出窗口的分组是" + base);
            dataMap.remove(base);
        }
    }

}
