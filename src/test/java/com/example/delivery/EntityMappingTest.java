package com.example.delivery;

import com.example.delivery.menu.entity.Menu;
import com.example.delivery.menu.repository.MenuRepository;
import com.example.delivery.order.entity.Order;
import com.example.delivery.order.entity.OrderStatus;
import com.example.delivery.order.repository.OrderRepository;
import com.example.delivery.payment.entity.Payment;
import com.example.delivery.payment.repository.PaymentRepository;
import com.example.delivery.user.entity.User;
import com.example.delivery.user.entity.UserRole;
import com.example.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EntityMappingTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    MenuRepository menuRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    PaymentRepository paymentRepository;

    @BeforeEach
    void clear() {
        // 외래키 때문에 자식 테이블부터 지운다
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        menuRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("회원·메뉴·주문·결제를 여러 개 저장하면 연관관계와 금액이 올바르게 저장된다")
    void saveAllEntities() {
        // given: 회원
        User owner1 = userRepository.save(new User("owner1", "password1234", UserRole.OWNER));
        User owner2 = userRepository.save(new User("owner2", "password1234", UserRole.OWNER));
        User cust1 = userRepository.save(new User("cust1", "password1234", UserRole.CUSTOMER));
        User cust2 = userRepository.save(new User("cust2", "password1234", UserRole.CUSTOMER));

        // given: 메뉴 (owner1이 2개, owner2가 1개)
        Menu kimbap = menuRepository.save(new Menu(owner1, "김밥", 3000L, "참치김밥"));
        Menu ramen = menuRepository.save(new Menu(owner1, "라면", 4000L, null));
        Menu tteok = menuRepository.save(new Menu(owner2, "떡볶이", 5000L, "매운맛"));

        // when: 주문
        Order order1 = orderRepository.save(new Order(kimbap, cust1, 2L, "서울시 강남구 1"));
        Order order2 = orderRepository.save(new Order(ramen, cust1, 1L, "서울시 강남구 1"));
        Order order3 = orderRepository.save(new Order(tteok, cust2, 3L, "서울시 마포구 2"));

        // when: 결제 (order1, order3만)
        Payment payment1 = paymentRepository.save(new Payment(order1));
        Payment payment3 = paymentRepository.save(new Payment(order3));

        // then: 개수
        check("users 개수", userRepository.count(), 4L);
        check("menus 개수", menuRepository.count(), 3L);
        check("orders 개수", orderRepository.count(), 3L);
        check("payments 개수", paymentRepository.count(), 2L);

        // then: 주문 총액 = 가격 × 수량 (D-07)
        check("order1.totalPrice (김밥 3000 × 2)", order1.getTotalPrice(), 6000L);
        check("order2.totalPrice (라면 4000 × 1)", order2.getTotalPrice(), 4000L);
        check("order3.totalPrice (떡볶이 5000 × 3)", order3.getTotalPrice(), 15000L);

        // then: 주문 기본 상태
        check("order1.status", order1.getStatus(), OrderStatus.ORDERED);

        // then: 결제 금액 = 주문 총액
        check("payment1.amount", payment1.getAmount(), order1.getTotalPrice());
        check("payment3.amount", payment3.getAmount(), 15000L);

        // then: 연관관계 따라가기
        check("payment3 → order → menu → owner", payment3.getOrder().getMenu().getOwner().getUsername(), "owner2");
        check("order1 → customer", order1.getCustomer().getUsername(), "cust1");
        check("ramen.description (선택 입력)", ramen.getDescription(), null);

        // then: 생성 시각 자동 기록
        checkNotNull("order1.createdAt", order1.getCreatedAt());
        checkNotNull("payment1.createdAt", payment1.getCreatedAt());
    }

    private <T> void check(String label, T actual, T expected) {
        assertThat(actual).as(label).isEqualTo(expected);
        System.out.printf("✅ %s = %s%n", label, actual);
    }

    private void checkNotNull(String label, Object actual) {
        assertThat(actual).as(label).isNotNull();
        System.out.printf("✅ %s = %s%n", label, actual);
    }
}