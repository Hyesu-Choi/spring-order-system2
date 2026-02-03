package com.beyond.ordersystem.common.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 사용자 정보 담을 스프링빈
@Component
public class SseEmitterRegistry {
    // SseEmitter 객체는 사용자의 연결정보(ip, macAddress 등)을 의미
//    ConcurrentHashMap은 Thread-Safe한 map(동시성 이슈 발생X)
    private Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

//    사용자 정보 저장
    public void addSseEmitter(String email, SseEmitter sseEmitter) {
        this.emitterMap.put(email, sseEmitter);
    }

//    사용자 정보 조회
    public SseEmitter getEmitter(String email) {
        return this.emitterMap.get(email);
    }

    public void removeEmitter(String email) {
        this.emitterMap.remove(email);
    }

}
