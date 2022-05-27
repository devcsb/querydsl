package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;
import java.util.function.Supplier;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/* 2. 사용자 정의 인터페이스 구현
 * 구현체의 이름은 반드시 스프링 데이터 JPA 인터페이스 명 + Impl 의 형식으로 작성해야 한다.
 *  */
public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    public MemberRepositoryImpl(JPAQueryFactory queryFactory) {
        super(Member.class);  /* doaminClass를 인자로 받는 부모, QuerydslRepositorySupport 생성자를 호출하여 부모의 여러 기능을 사용할 수 있음.*/
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        // queryFactory는 querydsl 4 버전에서 나온 개념
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)  // join(조인 대상, 대상의 Q타입)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    /**
     * 스프링 데이터 페이징 활용
     * content 쿼리를 페이징해서 가져오고 count 쿼리는 따로 구하는 방식.
     * content 쿼리와 count 쿼리를 분리하여 최적화가 가능하다.
     * count가 0이면 content 쿼리를 실행하지 않는다거나, 메서드로 분리해서 리팩토링 하는 등등..
     */
    @Override
    public Page<MemberTeamDto> searchPage(MemberSearchCondition condition, Pageable pageable) { // 스프링 데이터 jpa의 pageable 상속

        List<MemberTeamDto> content = queryFactory  // ctrl + alt + M으로 추출 가능
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .from(member)
                .leftJoin(member.team, team)  // join(조인 대상, 대상의 Q타입)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset()) // 몇 번째부터 시작할지
                .limit(pageable.getPageSize()) // 한 번 조회에 몇 개 까지 가져올지
                .fetch();

        //카운트를 구하는 쿼리. fetch() 없이 쿼리만 작성해서 PageableExecutionUtils로 넘긴다.
        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                ); // fetchCount()는 deprecated 되었으므로, 위와 같이 따로 count 구하는 쿼리를 날려서 사용.
        /*
         * 페이징 쿼리를 최적화 시켜주는 PageableExecutionUtils. getPage()
         * 1. 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때,
         * 2. 마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함)
         * 위의 경우, count 쿼리를 생략하고 content 쿼리만으로 count를 구한다.
         * */
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * QuerydslRepositorySupport를 활용한 querydsl 작성.
     * 장점 : 1. 페이징을 조금 편리하게 할 수 있다. 2. EntityManager를 제공한다.
     * 단점 : querydsl3의 문법. select로 시작할 수 없음. queryFactory를 제공하지 않음, 스프링 데이터 Sort기능이 정상 동작하지 않음.
     */
    public List<MemberTeamDto> search2(MemberSearchCondition condition) {

//        EntityManager entityManager = getEntityManager(); // 부모 추상클래스에서 EntityManager를 알아서 주입 받아주므로, 이렇게 가져와서 사용할 수 있다.

//      querydsl 3 버전은 from부터 시작했는데, QuerydslRepositorySupport는 그 때 만들어져서 from부터 시작함.
        return from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name))
                .fetch();
    }

    /**
     * QuerydslRepositorySupport를 활용하여 offset, limit 없이 페이징 하기
     * offset(pageable.getOffset()), limit(pageable.getPageSize()) 두 줄이 줄어드는 대신,
     * 메서드 체이닝이 끊기는 단점이 존재.
     */
    public Page<MemberTeamDto> searchPage2(MemberSearchCondition condition, Pageable pageable) { // 스프링 데이터 jpa의 pageable 상속

        JPQLQuery<MemberTeamDto> jpqlQuery = from(member)
                .leftJoin(member.team, team)  // join(조인 대상, 대상의 Q타입)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id,
                        team.name));

        // applyPagination 에서 페이지네이션 적용을 해준다.(offset(pageable.getOffset()), limit(...) 코드 생략)
        JPQLQuery<MemberTeamDto> query = getQuerydsl().applyPagination(pageable, jpqlQuery);

        List<MemberTeamDto> content = query.fetch();

        //카운트를 구하는 쿼리. fetch() 없이 쿼리만 작성해서 PageableExecutionUtils로 넘긴다.
        JPQLQuery<Long> countQuery = from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .select(member.count());

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }


    private BooleanBuilder usernameEq(String username) {
        return nullSafeBuilder(() -> member.username.eq(username));
    }

    private BooleanBuilder teamNameEq(String teamName) {
        return nullSafeBuilder(() -> team.name.eq(teamName));
    }

    private BooleanBuilder ageGoe(Integer ageGoe) {
        return nullSafeBuilder(() -> member.age.goe(ageGoe));
    }

    private BooleanBuilder ageLoe(Integer ageLoe) {
        return nullSafeBuilder(() -> member.age.loe(ageLoe));
    }

    /* Supplier : 인자를 받지 않고 Type T 객체를 리턴하는 함수형 인터페이스 T.get()으로 값 반환*/
    public static BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> f) {
        try {
            return new BooleanBuilder(f.get());
        } catch (IllegalArgumentException | NullPointerException e) {
            return new BooleanBuilder();
        }
    }


}
