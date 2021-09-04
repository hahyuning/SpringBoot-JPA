package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Embeddable;

@Embeddable
@Getter
public class Address {

    private String city;
    private String street;
    private String zipcode;

    // jpa 스펙에서 엔티티나 임베디드 타입은 자바 기본 생성자를 protected 로 설정
    protected Address() {
    }

    // 값 타입은 변경 불가능하게 설계
    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
