package com.beyond.ordersystem.common.configs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// spring에서 redis 접속하기 위한 bean 생성을 위한 코드
@Configuration
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;

    //    연결 빈 객체 : redis 연결 환경설정정보
    @Bean
//    Qualifier : 같은 Bean 객체가 여러개 있을 경우 Bean 객체를 구분하기 위한 어노테이션
    @Qualifier("rtInventory")  // 빈 객체에 이름 붙이기
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);  // 포트번호 yml에 관리
        configuration.setDatabase(0);  // 몇번 db  쓸껀지
        return new LettuceConnectionFactory();
    }

    @Bean
    @Qualifier("stockInventory")
    public RedisConnectionFactory stockConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);  // 포트번호 yml에 관리
        configuration.setDatabase(1);  // 몇번 db  쓸껀지
        return new LettuceConnectionFactory();
    }

    //    템플릿 빈 객체 : value에 어떤 자료구조로 넣을지 자료구조 설계. controller같은데에서 쓸 떄 이 빈을 주입받아서 쓰면 됨
    @Bean
    @Qualifier("rtInventory")
//    모든 template중에 무조건 redisTemplate이라는 메서드명이 반드시 1개는 있어야함.
    public RedisTemplate<String, String> redisTemplate(@Qualifier("rtInventory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());  // redis저장시 key는 string으로 만들어서 집어넣겠댜.
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("stockInventory")
    public RedisTemplate<String, String> stockRedisTemplate(@Qualifier("stockInventory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());  // redis저장시 key는 string으로 만들어서 집어넣겠댜.
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }


}
