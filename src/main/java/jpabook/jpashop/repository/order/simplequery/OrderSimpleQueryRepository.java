package jpabook.jpashop.repository.order.simplequery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    public List<OrderSimpleQueryDTo> findOrderDtos() {
        // 일반적인 sql을 사용할 때처럼 원하는 값을 선택해서 조회
        // new 명령어를 사용해서 jpql 결과를 dto로 즉시 변환
        // select 절에서 원하는 데이터를 직접 선택하므로 네트워크 용량 최적화 (생각보다 미비)
        // 리포지토리 재사용성이 떨어지고, api 스펙에 맞춘 코드가 리포지토리에 들어간다는 단점
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderSimpleQueryDTo.class)
                .getResultList();
    }
}
