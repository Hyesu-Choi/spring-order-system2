package com.beyond.ordersystem.common.controller;

import com.beyond.ordersystem.common.repository.SseEmitterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/sse")
public class SseController {
    private final SseEmitterRegistry sseEmitterRegistry;
    @Autowired
    public SseController(SseEmitterRegistry sseEmitterRegistry) {
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @GetMapping("/connect")
    public SseEmitter connect() throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        SseEmitter sseEmitter = new SseEmitter(60*60*1000L); //1시간 유효시간. 사용자 고유 객체라서 싱글톤으로 만들면 안됨
        sseEmitterRegistry.addSseEmitter(email, sseEmitter);  // 요청들어온 사용자 정보로 에미터 객체 만듬
        sseEmitter.send(SseEmitter.event().name("connect").data("연결완료"));  // 응담 보내기
        return sseEmitter;
    }


}
