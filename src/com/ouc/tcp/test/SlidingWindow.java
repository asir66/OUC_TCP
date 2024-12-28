package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindow {
    volatile int base = 1; // 窗口基
    volatile int windowSize = 16; // 窗口大小N ||更严格来说应该是缓冲区大小
    volatile int cwnd = 1; // 拥塞窗口大小
    volatile int ssthresh = windowSize / 2; // 慢启动阈值
    int singlePacketSize = 100; // 单个数据包大小
    int finalSeq = 99901;
    // 利用集合的方式存放数据包
    ConcurrentHashMap<Integer, TCP_PACKET> dataMap = new ConcurrentHashMap<Integer, TCP_PACKET>();

    // 构造函数
    public SlidingWindow() {

    }

    public boolean isFull() {
        return dataMap.size() >= cwnd;
    }

    void slide(int ack){
    }
}
