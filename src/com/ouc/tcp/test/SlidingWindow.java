package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindow {
//    volatile int base = 1; // 窗口基
    volatile int windowSize = 16; // 窗口大小N
    int singlePacketSize = 100; // 单个数据包大小
    // 利用集合的方式存放数据包
    ConcurrentHashMap<Integer, TCP_PACKET> dataMap = new ConcurrentHashMap<Integer, TCP_PACKET>();

    // 构造函数
    public SlidingWindow() {

    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getDataMapSize() {
        return dataMap.size();
    }

    public boolean isFull() {
        return dataMap.size() >= windowSize;
    }

    public boolean isEmpty() {
        return dataMap.isEmpty();
    }

    void slide(int ack){

    }
}
