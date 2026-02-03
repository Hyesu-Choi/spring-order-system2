package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.service.SseAlarmService;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dtos.OrderCreateDto;
import com.beyond.ordersystem.ordering.dtos.OrderListDto;
import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final SseAlarmService sseAlarmService;

    public OrderingService(OrderingRepository orderingRepository, OrderDetailRepository orderDetailRepository, ProductRepository productRepository, MemberRepository memberRepository, SseAlarmService sseAlarmService) {
        this.orderingRepository = orderingRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
        this.sseAlarmService = sseAlarmService;
    }

    public Long create(List<OrderCreateDto> orderCreateDtoList) {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("member is not found"));

        Ordering ordering = Ordering.builder()
                .member(member)
                .build();
        for (OrderCreateDto dto : orderCreateDtoList) {
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new EntityNotFoundException("entity is not found"));

            if (product.getStockQuantity() < dto.getProductCount()) {
                throw new IllegalArgumentException("재고가 부족합니다.");
            }

            product.updateStockQuantity(dto.getProductCount());

            OrderDetail orderDetail = OrderDetail.builder()
                    .ordering(ordering)
                    .product(product)
                    .quantity(dto.getProductCount())
                    .build();
            ordering.getOrderDetailList().add(orderDetail);
        }
        orderingRepository.save(ordering);

//        주문 성공시 admin 유저에게 알림메시지 전송 .. receiver 동적으로 설계
        String message = ordering.getId() + "번 주문이 들어왔습니다.";
//        알림 DB저장 로직 추가 하면됨
//        alarmRepository.save(alram객체) 이런식으로 db에 알림 저장하고, 메시지 보내기
        sseAlarmService.sendMessage("admin@naver.com", email, message);


        return ordering.getId();
    }


    @Transactional(readOnly = true)
    public List<OrderListDto> findAll() {
        return orderingRepository.findAll().stream().map(o -> OrderListDto.fromEntity(o)).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<OrderListDto> myorders() {
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("member is not found"));
        return orderingRepository.findAllByMember(member).stream().map(o -> OrderListDto.fromEntity(o)).collect(Collectors.toList());
    }

}
