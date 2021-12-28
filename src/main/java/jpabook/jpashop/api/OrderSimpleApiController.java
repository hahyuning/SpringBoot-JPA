package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDTo;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 *
 * xToOne(ManyToOne, OneToOne) 관계 최적화
 * Order
 * Order -> Member
 * Order -> Delivery
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    // v1: 엔티티 직접 노출
    // - Hibernate5Module 등록, LAZY=null 처리
    // - 양방향 관계 문제 발생 -> @JsonIgnore
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {

        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기환
        }
        return all;
    }

    // v2: 엔티티를 조회해서 dto로 변환 (fetch join 사용 x)
    // 단점: 지연로딩으로 쿼리 N번 호출
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {

        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // Lazy 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // Lazy 초기화
        }
    }

    // v3: 엔티티를 조회해서 dto로 변환 (fetch join 사용 o)
    // fetch join으로 쿼리 1번 호출
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {

        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(toList());
    }

    // v4: jpa에서 dto로 바로 조회
    // - 쿼리 1번 호출
    // - select 절에서 원하는 데이터만 선택해서 조회
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDTo> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

    // 정리: 쿼리 방식 선택 권장 순서
    // 1. 우선 엔티티를 dto로 변환하는 방법을 선택한다.
    // 2. 필요하면 페치 조인으로 성능을 최적화 한다. -> 대부분의 성능 이슈가 해결된다.
    // 3. 그래도 안되면 dto로 직접 조회하는 방법을 사용한다.
    // 4. 최후의 방법은 jpa가 제공하는 네이티브 sql이나 스프링 jdbc template를 사용해서 sql을 직접 사용한다.
}
