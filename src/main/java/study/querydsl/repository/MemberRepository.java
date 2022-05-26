package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> { // JpaRepository<Entity 클래스의 타입, ID의 타입>을 상속하면 기본적인 CRUD 메소드 자동 생성

    // SELECT m FROM Member m WHERE m.username = ?  // 메소드 이름으로 왼쪽의 jpql을 만들어준다.
    List<Member> findByUsername(String username);
}
