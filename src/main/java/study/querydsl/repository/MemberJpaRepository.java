package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

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

    /*
     * 동적쿼리 사용한 조회 (Builder 사용)
     * 동적쿼리를 생성하는 코드를 짤 때, 쿼리 조건이 모두 null인 경우 모든 데이터를 다 가져오므로,
     * 기본조건을 넣어주거나, 최소한 limit을 걸어서 페이징 쿼리를 추가해서 날리는 편이 좋다.
     *
     */
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();
        if (hasText(condition.getUsername())) {  // null, ""(빈문자열) 필터링 위해 StringUtils.hasText() 사용.
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id, // Q파일 사용하므로, Dto 필드명이 달라도 따로 .as("Dto필드명")으로 명시할 필요가 없다!
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)  // join(조인 대상, 대상의 Q타입)
                .where(builder)
                .fetch();
    }

}
