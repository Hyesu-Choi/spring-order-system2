package com.beyond.ordersystem.common.configs;

import com.beyond.ordersystem.common.service.SseAlarmService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// spring에서 redis 접속하기 위한 bean 생성을 위한 코드
@Configuration
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;

    //    연결 Bean 객체 : redis 연결 환경설정정보
    @Bean
//    Qualifier : 같은 Bean 객체가 여러개 있을 경우 Bean 객체를 구분하기 위한 어노테이션
    @Qualifier("rtInventory")  // 빈 객체에 이름 붙이기
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);  // 포트번호 yml에 관리
        configuration.setDatabase(0);  // 몇번 db  쓸껀지. 최근에는 0번만 쓰는게 트렌드고 여러개의 테이블이 필요할 경우 docker에 redis 여러개 띄우는게 트렌드
        return new LettuceConnectionFactory();
    }

    @Bean
    @Qualifier("stockInventory")
    public RedisConnectionFactory stockConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(1);
        return new LettuceConnectionFactory();
    }


    //    템플릿 Bean 객체 : value에 어떤 자료구조로 넣을지 자료구조 설계. controller같은데에서 쓸 떄 이 빈을 주입받아서 쓰면 됨
    @Bean
    @Qualifier("rtInventory")
//    모든 template중에 무조건 redisTemplate이라는 메서드명이 반드시 1개는 있어야함.
//    @Bean객체 생성시, bean 객체간에 DI(의존성주입)는 "메서드 파라미터 주입" 이 가능
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
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }


    //    redis pub/sub관런
    @Bean
    @Qualifier("ssePubSub")
    public RedisConnectionFactory ssePubSubConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
//        redis pub/sub기능은 db에 값을 저장하는 기능이 아니므로, 특정 db에 의존적이지 않음. - db세팅 필요없음
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(1);
        return new LettuceConnectionFactory();
    }

    @Bean
    @Qualifier("ssePubSub")
    public RedisTemplate<String, String> ssePubSubRedisTemplate(@Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

//    호출 흐름 : RedisMessageListenerContainer -> MessageListenerAdapter -> SseAlarmService(MessageListener)
//    redis 리스너(subscribe) 객체
//    SUBSCRIBE order_channel 느낌 : 채널 구독
    @Bean
    @Qualifier("ssePubSub")
    public RedisMessageListenerContainer redisMessageListenerContainer(@Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory, @Qualifier("ssePubSub") MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic("order-channel"));  //메시지처리객체 호출
//        만약에 여러 채널을 구독해야 하는 경우, 여러개의 PatternTopic을 add하거나, 별도의 Listener Bean 객체 생성
        return container;
    }

//    redis에서 수신된 메시지를 처리하는 객체
//    PUBLISH order_channel "Hello, this is a test message"  : order-channel 구독하고있는 모든 서버에게 메세지 보냄
    @Bean
    @Qualifier("ssePubSub")
    public MessageListenerAdapter messageListenerAdapter(SseAlarmService sseAlarmService) {
//        채널로부터 수신되는 message 처리를 SseAlarmService의 onMessage메서드로 위임.
        return new MessageListenerAdapter(sseAlarmService, "onMessage");
    }


}
