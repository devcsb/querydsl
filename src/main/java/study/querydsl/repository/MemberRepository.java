package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

// 상속받은 각 인터페이스의 역할
// - JpaRepository<Entity 클래스의 타입, ID의 타입>을 상속하면 기본적인 CRUD 메소드 자동 생성
/* - MemberRepositoryCustom : 커스텀 Repository 작성 순서 3. 스프링 데이터 리포지토리에 사용자 정의 인터페이스 상속 */
// - QuerydslPredicateExecutor <타입> : 스프링 데이터 JPA가 제공하는 Querydsl 기능 사용하기 위함.
// (ex. findAll(QMember.member) 이런 식으로 querydsl 작성을 spring-data-jpa 형식으로 가능하게 하는 기능)
// left join 사용 X, 레이어 아키텍쳐 위배 등으로 실무에서 사용하기 힘듦.
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {

    // SELECT m FROM Member m WHERE m.username = ?  // 메소드 이름으로 왼쪽의 jpql을 만들어준다.
    List<Member> findByUsername(String username);
}
