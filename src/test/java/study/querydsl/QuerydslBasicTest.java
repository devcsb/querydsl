package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);  //EntityManager 자체에서 멀티쓰레드 동시성 이슈 없이 동작하도록 설계되어있으므로, 필드로 빼내서 사용해도 무방하다.

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception {
        //member1을 찾아라.
        String qlString = "select m from Member m" +
                " where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)  // ctrl + alt + V 로 변수 추출
                .setParameter("username", "member1")
                .getSingleResult();  //결과가 하나일 때 사용. 없거나 한개 이상이면 Exception 발생

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuesydsl() throws Exception {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);  // 1. JPAQueryFactory 생성 .. 필드로 빼내고, beforeg함수에서 생성
//        QMember m = new QMember("m1");  // 2. Qmember 생성. // 같은 테이블을 join 하는 경우에만 구분을 위해 이렇게 선언해서 사용하고, 보통은 기본 QType을 사용

        Member findMember = queryFactory
                .select(member)  // 따로 선언 없이 기본 Q-type인 Qmember.member를 static import하여 사용.
                .from(member)
                .where(member.username.eq("member1"))  // Querydsl이 JDBC prepareStatement 로 자동으로 파라미터 바인딩 해준다.
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)  //select 와 From을 합친 문법
                .where(
                        member.username.eq("member1")
                                .and(member.age.eq(10))  // 방법 1 : and로 체이닝해서 조건을 주는 방법.
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)  //select 와 From을 합친 문법
                .where(
                        // 방법 2 : 쉼표로 끊어서, 파라미터를 가변적으로 줄 수 있다. 값에 null이 있으면 null을 찾는 조건이 무시되므로, 동적쿼리 작성에 용이하다.
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest() throws Exception {
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건 조회. 없으면 null, 둘 이상이면 Exception 발생
        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.age.loe(10))
                .fetchOne();

        //처음 한 건 조회 <limit(1)>
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();  // == limit(1).fetchOne();

        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();  //select count 와 content를 가져오는 쿼리 2번이 날아간다.

        results.getTotal();
        List<Member> content = results.getResults();

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 오름차순(asc)
     * 단, 2 에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() throws Exception {
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) //null값은 마지막에
                .fetch();

        //then
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * 조회 건수 제한하여 페이징
     */
    @Test
    public void paging1() throws Exception {
        //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        //then
        assertThat(result.size()).isEqualTo(2);
    }

    /**
     * 전체 조회 수가 필요할 때 fetchResults를 사용한 페이징
     */
    @Test
    public void paging2() throws Exception {
        //given
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();  //deprecated. 따로 count 쿼리를 날리자!

        //then
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * 따로 작성하는 count 쿼리 예제.
     */
    @Test
    public void count() {
        Long totalCount = queryFactory
//                .select(Wildcard.count) // select count(*)
                .select(member.count()) // select count(member.id)
                .from(member)
                .fetchOne(); //숫자 하나이므로 fetchOne()

        System.out.println("totalCount = " + totalCount);
    }

}
