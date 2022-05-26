package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

// JpaRepository<Entity 클래스의 타입, ID의 타입>을 상속하면 기본적인 CRUD 메소드 자동 생성
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom { /* 3. 스프링 데이터 리포지토리에 사용자 정의 인터페이스 상속 */

    // SELECT m FROM Member m WHERE m.username = ?  // 메소드 이름으로 왼쪽의 jpql을 만들어준다.
    List<Member> findByUsername(String username);
}
