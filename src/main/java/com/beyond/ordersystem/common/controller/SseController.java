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
        SseEmitter sseEmitter = new SseEmitter(60*60*1000L); // 1시간 유효시간. 사용자 고유 객체라서 싱글톤으로 만들면 안됨.계속 연결상태에 있으면 서버 과부화와서 서버쪽에서 1시간뒤면 자동으로 끊기게 코드 추가함. 프론트쪽에서도 추가해줘야함.
        sseEmitterRegistry.addSseEmitter(email, sseEmitter);  // 요청들어온 사용자 정보로 sse 객체 만듬
        sseEmitter.send(SseEmitter.event().name("connect").data("연결완료"));  // 연결 성공 응답 보내기
        return sseEmitter;
    }

    @GetMapping("/disconnect")
    public void disconnect() throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        sseEmitterRegistry.removeEmitter(email);  // 사용자 정보 삭제
    }


}
