package com.beyond.ordersystem.common.auth;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@Component
public class JwtTokenProvider {
    @Value("${jwt.secretKey}")
    private String st_secret_key;
    @Value("${jwt.expiration}")
    private int expiration;
    private Key secret_key;


    @Value("${jwt.secretKeyRt}")
    private String st_secret_key_rt;
    @Value("${jwt.expirationRt}")
    private int expiration_rt;
    private Key secret_key_rt;

    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;

    @Autowired
    public JwtTokenProvider(@Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate, MemberRepository memberRepository) {
        this.redisTemplate = redisTemplate;
        this.memberRepository = memberRepository;
    }

    @PostConstruct
    public void init() {
        secret_key = new SecretKeySpec(Base64.getDecoder().decode(st_secret_key), SignatureAlgorithm.HS512.getJcaName());
        secret_key_rt = new SecretKeySpec(Base64.getDecoder().decode(st_secret_key_rt), SignatureAlgorithm.HS512.getJcaName());
    }

    public String createToken(Member member) {
        Claims claims = Jwts.claims().setSubject(member.getEmail());
        claims.put("role", member.getRole().toString());

        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 60 * 1000L)) //30분*60초*1000밀리초 : 밀리초형태로 변환
                .signWith(secret_key)
                .compact();
        return token;
    }

//    rt 생성
    public String createRtToken(Member member) {
//        유효기간이 긴 rt 토큰 생성
        Claims claims = Jwts.claims().setSubject(member.getEmail());
        claims.put("role", member.getRole().toString());

        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration_rt * 60 * 1000L))
                .signWith(secret_key_rt)
                .compact();

//        rt토큰을 redis에 저장
//        opsForValue : 일반 스트링 자료 구조. opsForSet(또는 Zset 또는 List 등) 존재
//        redisTemplate.opsForValue().set(member.getEmail(), token);
//        {회원이메일:토큰값} 3000분 뒤에 자동삭제되게 데이터 담겠다는 의미.
        redisTemplate.opsForValue().set(member.getEmail(), token, expiration_rt, TimeUnit.MINUTES);  // 3000분 ttl

        return token;
    }

//        rt토큰 그 자체를 검증
    public Member validateRt(String refreshToken) {
        Claims claims = null;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(st_secret_key_rt)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 토큰입니다.");
        }

        String email = claims.getSubject();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("이메일 찾을 수 없음"));
//        redis rt와 비교 검증
        String redisRt = redisTemplate.opsForValue().get(email);  //이메일값이랑 일치하는 rt토큰 꺼내오기.
        if (!redisRt.equals(refreshToken)) {
            throw new IllegalArgumentException("잘못된 토큰입니다.");
        }

        return member;

    }


};
