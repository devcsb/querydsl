package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;  // 스프링에서 EntityManager는 프록시 사용하여 멀티쓰레드 환경에서 동시성 문제 해결해줌.  책 13.1챕터 참고.
    private final JPAQueryFactory queryFactory;  // JPAQueryFactory의 동시성 문제는 EntityManager에 의존하므로. 역시 문제없음.

    /* 생성자에서 생성해서 쓰는 방식 외부에서 주입받는 객체가 적으므로, 테스트 시 조금 덜 번거롭다*/
//    public MemberJpaRepository(EntityManager em) {
//        this.em = em;
//        this.queryFactory = new JPAQueryFactory(em); // 생성자에서 생성해서 쓰는 방식
//    }

    /* 스프링 빈으로 등록한 걸 받아서 쓰는 방식. 롬복의 @RequiredArgsConstructor 로 편하게 대체할 수 있다.*/
//    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//        this.em = em;
//        this.queryFactory = queryFactory;
//    }

    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username",username)
                .getResultList();
    }

    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

}
