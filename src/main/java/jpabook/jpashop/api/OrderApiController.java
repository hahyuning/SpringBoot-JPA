package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    // v1: 엔티티 직접 노출
    // - 엔티티가 변하면 api 스펙이 변한다.
    // - 트랜잭션 안에서 지연로딩 필요
    // - 양방향 연관관계 문제
    @GetMapping("api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        for (Order order : all) {
            order.getMember().getName(); // Lazy 강제 초기화
            order.getDelivery().getAddress(); // Lazy 강제 초기화
            
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.forEach(o -> o.getItem().getName()); // Lazy 강제 초기화
        }
        return all;
    }

    // v2: 엔티티를 조회해서 dto로 변환 (fetch join 사용 x)
    // - 트랜잭션 안에서 지연로딩 필요
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        // List<OrderItem> 대신에 List<OrderItemDto>를 사용해야 한다.
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(Collectors.toList());
        }
    }

    @Data
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

    // v3: 엔티티를 조회해서 dto로 변환 (fetch join 사용 o)
    // - 페이징 시에는 N 부분을 포기해야 한다.
    // - 대신에 batch fetch size? 옵션을 주면 N -> 1 쿼리로 변경 가능
    //
    // 단점: 컬렉션을 페치 조인하면 페이징이 불가능하다.
    // - 컬렉션을 페치 조인하면 일대다 조인이 발생하므로 데이터가 예측할 수 없이 증가한다.
    // - 일다대에서 일(1)을 기준으로 페이징을 하는 것이 목적이다. 그런데 데이터는 다(N)를 기준으로 row가 생성된다.
    // - Order를 기준으로 페이징 하고 싶은데, 다(N)인 OrderItem을 조인하면 OrderItem이 기준이 되어버린다.
    // - 이 경우 하이버네이트는 경고 로그를 남기고 모든 DB 데이터를 읽어서 메모리에서 페이징을 시도하고, 최악의 경우 장애로 이어질 수 있다.
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    // v3.1: 엔티티를 조회해서 dto로 변환 (페이징 고려)
    // - ToOne 관계만 우선 페치 조인으로 최적화
    // - 컬렉션 관계는 hibernate.default_batch_fetch_size , @BatchSize로 최적화
    //    (hibernate.default_batch_fetch_size: 글로벌 설정, @BatchSize: 개별 최적화)
    // - 이 옵션을 사용하면 컬렉션이나, 프록시 객체를 한꺼번에 설정한 size 만큼 IN 쿼리로 조회한다.
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    // 정리
    // v3.1의 장점
    // - 쿼리 호출 수가 1 + N 1 + 1 로 최적화 된다.
    // - 조인보다 DB 데이터 전송량이 최적화 된다.
    // (Order와 OrderItem을 조인하면 Order가 OrderItem 만큼 중복 조회되지만 이 방법은 각각 조회하므로 전송해야할 중복 데이터가 없다.)
    // - 페치 조인 방식과 비교해서 쿼리 호출 수가 약간 증가하지만, DB 데이터 전송량이 감소한다.
    // - 컬렉션 페치 조인은 페이징이 불가능 하지만 이 방법은 페이징이 가능하다.
    //
    // 결론: ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다. 따라서 ToOne 관계는 페치조인으로쿼리 수를 줄이고 해결하고,
    // 나머지는 hibernate.default_batch_fetch_size 로 최적화
}

