package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

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
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);  // 1. JPAQueryFactory 생성 .. 필드로 빼내고, before 함수에서 생성
//        QMember m = new QMember("m1");  // 2. Qmember 생성. // 같은 테이블을 join, 서브쿼리 사용하는 경우에만 구분을 위해 이렇게 선언해서 사용하고, 보통은 기본 QType을 사용

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

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        //given
        List<Tuple> result = queryFactory  //Querydsl 튜플로 반환. 데이터 타입이 여러개이므로...  // 실무에서는 Dto로 직접 뽑아오는 방식을 많이 사용한다.
                .select(
                        member.count(), // 회원 수
                        member.age.sum(),  // 나이 합
                        member.age.avg(),  // 평균 나이
                        member.age.max(),  // 최대 나이
                        member.age.min()  // 최소 나이
                )
                .from(member)
                .fetch();

        //then
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        //when
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);  // (10 + 20) / 2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
    }

    /**
     * 팀 A에 소속된 모든 회원을 찾기
     */
    @Test
    public void join() throws Exception {
        //given
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)  // join(조인 대상, 별칭으로 사용할 Q타입)  // leftJoin(), rightJoin() 가능
                .where(team.name.eq("teamA"))
                .fetch();

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 외부 조인 불가능 -> 조인 on을 사용하면 외부 조인 가능
     */
    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Member> result = queryFactory
                .select(member)  // from 절에 여러 엔티티를 선택해서 세타 조인
                .from(member, team) // 모든 member와 모든 team을 가져와서 where절에서 필터링하는 것이라 이해하면 된다. (물론 db가 성능최적화를 해준다.)
                .where(member.username.eq(team.name))
                .fetch();

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }


    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA")) // join일 경우 조인대상(team)을 필터링하는 on 절
//                .where(team.name.eq("teamA"))  // inner join일 때는 on절을 익숙한 where절로 대체 가능
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
//     결과:  tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
//            tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
//            tuple = [Member(id=5, username=member3, age=30), null]
//            tuple = [Member(id=6, username=member4, age=40), null]
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 예 ) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL : SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL : SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     */
    @Test
    public void join_on_no_relation() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team)  //연관관계 있는 경우의 조인. 이렇게 하면 join on절에 id값이 들어가면서 매칭된다. join(조인대상, alias Q타입.)
                .leftJoin(team) // 막 조인 하는 경우. 조인대상 엔티티의 Qtype만 명시.
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

//  결과: t=[Member(id=3, username=member1, age=10), null]
//        t=[Member(id=4, username=member2, age=20), null]
//        t=[Member(id=5, username=member3, age=30), null]
//        t=[Member(id=6, username=member4, age=40), null]
//        t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
//        t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]

    }


    @PersistenceUnit
    EntityManagerFactory emf;

    /**
     * 페치 조인 미적용
     * 지연로딩으로 Member, Team SQL 쿼리 각각 실행
     */
    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); //해당 객체가 이미 로딩된 엔티티인지 아닌지 판별
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    /**
     * 페치 조인 적용
     * 즉시로딩으로 Member, Team SQL 쿼리 조인으로 한번에 조회
     */
    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() //연관된 team을 한 쿼리로 한번에 끌고온다.
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); //해당 객체가 이미 로딩(초기화)된 엔티티인지 아닌지 판별
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 서브 쿼리 예제 [com.querydsl.jpa.JPAExpressions 사용]
     * 나이가 가장 많은 회원 조회 (eq 사용) eq == equal == "="
     */
    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");  //select 하는 member와 alias가 겹치면 안되므로, 다른 별칭을 가지는 QMember 직접 생성

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions  //static import 가능
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 나이 이상인 회원 (goe 사용) goe == grater or equal == ">="
     */
    @Test
    public void subQueryGoe() throws Exception {

        QMember memberSub = new QMember("memberSub");  //select 하는 member와 alias가 겹치면 안되므로, 다른 별칭을 가지는 QMember 직접 생성

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions  // static import 가능
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * 서브쿼리 여러 건 처리 (in 사용)
     */
    @Test
    public void subQueryIn() throws Exception {

        QMember memberSub = new QMember("memberSub");  //select 하는 member와 alias가 겹치면 안되므로, 다른 별칭을 가지는 QMember 직접 생성

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions  // static import 가능
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    /**
     * select절에 서브쿼리 사용.
     */
    @Test
    public void selectSubquery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");  //select 하는 member와 alias가 겹치면 안되므로, 다른 별칭을 가지는 QMember 직접 생성

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions  // static import 가능
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
     * JPA JPQL의 한계로, From절의 서브쿼리(인라인뷰)는 사용할 수 없다.
     *
     * from 절의 서브쿼리 해결방안
     * 1. 서브쿼리를 join으로 변경한다.(대부분 변경 가능)
     * 2. 어플리케이션에서 쿼리를 2개로 분리해서 실행한다.
     * 3. nativeSQL을 사용한다.
     *
     * 화면에 맞춘 비즈니스 로직을 SQL문에 녹여내다보면 자연스레 From절 서브쿼리를 사용하게 되는데, 잘못된 방법이다.
     * DB는 데이터를 최소화해서 필터링, 그루핑해서 퍼올리는 용도로만 써야한다.
     * 복잡한 한방쿼리보다 쿼리를 여러번 나눠서 보내는 것이 훨씬 낫다.
     * */

    /**
     * Case 문
     * select, 조건절(where), order by에서 사용 가능
     * 단순한 조건의 경우. QType의 필드값에서. when ~ then
     */
    @Test
    public void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 복잡한 조건의 경우. new CaseBuilder() 생성하여 . when(QType.속성~) ~ then ~
     *
     * ********** case문 대신, row data를 가져와서 애플리케이션 단에서 처리하자! **********
     */
    @Test
    public void complexCase() throws Exception {

        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21살~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 예를 들어서 다음과 같은 임의의 순서로 회원을 출력하고 싶다면?
     * 1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     * 2. 0 ~ 20살 회원 출력
     * 3. 21 ~ 30살 회원 출력
     */
    @Test
    public void orderByCase() throws Exception{

        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);  // rankPath 조건을 변수로 선언

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)  //select절에서 사용
                .from(member)
                .orderBy(rankPath.desc())  //orderBy절에서 사용
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = "
                    + rank);
        }
    }

    /**
     * 상수를 가져오기 Expressions.constant(xxx) 사용
     */
    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        //위와 같이 최적화가 가능하면 SQL에 constant 값을 넘기지 않는다. 상수를 더하는 것 처럼 최적화가
        //어려우면 SQL에 constant 값을 넘긴다.
    }

    /**
     * 문자열 더하기 concat
     * member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로
     * 문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다.
     */
    @Test
    public void concat() throws Exception{
        //{username}_{age} 형태로 표시하려한다.
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) // age의 타입을 string으로 맞춰주고 concat시킨다.
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

            System.out.println("result = " + result);
    }

    /**
     * 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음
     * 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
     */
    @Test
    public void simpleProjection() throws Exception{
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 튜플 조회 => 프로젝션 대상이 둘 이상일 때 사용
     * 튜플은 querydsl 객체. 도메인 계층을 벗어나서 사용하는 것은 좋지 않다.
     */
    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username); // 튜플에서 값 꺼내기
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * 순수 JPA에서 DTO 조회
     * - 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
     * - DTO의 package 이름을 다 적어줘야해서 지저분함
     * - 생성자 방식만 지원함
     */
    @Test
    public void findDtoByJPQL() throws Exception{

//        em.createQuery("select m.username, m.age from Member m", MemberDto.class);  // JPQL이 Member Entity를 조회하므로 리턴 타입이 맞지 않다!
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl 빈 생성 - 방법 1 : 프로퍼티 접근 - Setter 활용
     *
     * Projections.bean(타입, 꺼내올 값(...))
     * 조회한 결과값을 bean에 담아,setter로 Dto에 injection 하고, getter로 다시 꺼내온다.
     */
    @Test
    public void findDtoBySetter() throws Exception {

        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl Bean 생성 - 방법 2 : 필드 직접 접근
     * Projections.fields(타입, 꺼내올 값(...)) // 필드에 바로 주입
     */
    @Test
    public void findDtoByField() throws Exception {

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,  // 필드에 직접 접근. getter, setter가 필요없다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * name이라는 필드명을 가진 UserDto를 사용하려면, Qmember의 username과 별칭이 다르므로 에러 발생.
     * 별칭이 다를 때, .as("Dto의 필드명")으로 직접 지정해주면 된다.
     */
    @Test
    public void findUserDto() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,  // 필드에 직접 접근. getter, setter가 필요없다.
                        member.username.as("name"),  // .as("Dto 필드명")

                        ExpressionUtils.as(JPAExpressions  // ExpressionUtils.as(서브쿼리, alias) 로 서브쿼리에 별칭을 지정해준다
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                        ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * Querydsl Bean 생성 - 방법 3 : 생성자 접근
     * Projections.constructor(타입, 꺼내올 값(...))  // Dto 필드 타입의 순서에 맞게 조회해야 한다.
     */
    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,  // MemberDto의 타입에 맞게 순서를 맞춰서 조회해야한다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 방법 3 : 생성자 접근 - UserDto 활용. 생성자 접근 방식은 타입으로 판별하기 떄문에 타입의 순서만 잘 맞춰주면 된다.
     */
    @Test
    public void findUserDtoByConstructor() throws Exception {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,  // UserDto의 타입에 맞게 순서를 맞춰서 조회해야한다.
                        member.username,
                        member.age))  //ex)... , member.id)) // Dto 필드에 없는 값을 추가로 조회하려고 해도, 컴파일 단계에선 알 수 없으므로, 런타임 단계에서 오류가 발생한다.
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /*
     * @QueryProjection 활용 (생성자를 활용 + Q파일 활용)
     * 이 방법은 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다. 다만 DTO에 QueryDSL
     * 어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있다.
     */
    @Test
    public void findDtoByQueryProjection() throws Exception{

        List<MemberDto> result = queryFactory
//                .select(new QMemberDto(member.username, member.age, member.id)) // 컴파일 단계에서 오류 발생! Q파일의 생성자에서 이미 username와 age만 받을 수 있게 선언되어있으므로.
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 동적 쿼리 - BooleanBuilder 사용
     */
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder(); // new BooleanBuilder(member.username.eq(usernameCond)); 처럼 초기값 세팅 가능
        if (usernameCond != null) { // username 값이 있으면
            builder.and(member.username.eq(usernameCond)); //빌더가 쿼리문 생성
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적 쿼리 - Where 다중 파라미터 사용
     * - where 조건에 null 값은 무시된다.
     * - 메서드로 추출한 조건을 다른 쿼리에서도 재활용 할 수 있다.
     * - 쿼리 자체의 가독성이 높아진다.
     */
    @Test
    public void dynamicQuery_WhereParam() throws Exception{
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond),ageEq(ageCond)) // 가독성이 매우 좋다.
//                .where(allEq(usernameCond, ageCond)) //조건을 조합한 조건을 사용가능
                .fetch();
    }
    // 재사용할 수 있도록 메서드를 추출하여 사용
    private BooleanBuilder usernameEq(String usernameCond) {
//        return usernameCond != null ? member.username.eq(usernameCond) : null; // where 절에 null은 무시되므로, 자연스레 동적쿼리가 만들어짐
        //값이 null일 경우에도 new Booleanbuilder();를 반환하여 계속적으로 조건을 체이닝할 수 있도록 개선하였음.
        if (usernameCond == null) {
            return new BooleanBuilder();
        } else {
            return new BooleanBuilder(member.username.eq(usernameCond));
        }
    }

    private BooleanBuilder ageEq(Integer ageCond) {
//        return ageCond != null ? member.age.eq(ageCond) : null;
        if (ageCond == null) {
            return new BooleanBuilder();
        } else {
            return new BooleanBuilder(member.age.eq(ageCond));
        }
    }

    // 검색조건 조합 가능 (usernameCond가 null경우, 뒤의 조건에 체이닝이 되지 않음. 이를 해결한 자세한 코드는 DynamicQueryTest에 따로 작성하였음.)
    private BooleanBuilder allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 벌크연산은 영속성컨텍스트를 거치지 않고 DB에 바로 연산하므로, 영속성 컨텍스트는 그대로이다. -> 트랜잭션 내에서 db와 영속성 컨텍스트 불일치 문제 발생!
     */
    @Test
    public void bulkUpdate() throws Exception {

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 벌크연산 수행으로 DB에서 값이 이렇게 변경됨
        // 1 member1 -> 1 DB 비회원
        // 2 member2 -> 2 DB 비회원
        // 3 member3 -> 3 DB member3
        // 4 member4 -> 4 DB member4

        /* 따라서 벌크연산시, db와 영속성컨텍스트 불일치 문제 해결을 위해 flush, clear로 초기화 해주자! */

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        /* db에서 가져온 값을 영속성컨텍스트에 넣어줘야하는데, 이미 영속성 컨텍스트에는 member1,2,3,4가 모두 있음.
        * 영속성 컨텍스트에 중복값(id로 판별)이 이미 있으면, DB에서 가져온 값은 버리고, 영속성 컨텍스트의 값을 그대로 유지시킨다.
        * */

        for (Member member : result) {
            System.out.println("member = " + member);
        }
        // JPA 는 1차 캐시를 통해서 데이터베이스가 아닌 애플리케이션 레벨에서 Repeatable Read를 제공
        // 영속성 컨텍스트에서 가져온 값 :
//        member = Member(id=3, username=member1, age=10)
//        member = Member(id=4, username=member2, age=20)
//        member = Member(id=5, username=member3, age=30)
//        member = Member(id=6, username=member4, age=40)
    }

    /*벌크연산 - 값에 곱하기, 더하기 연산 */
    @Test
    public void bulkAdd() throws Exception{
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
//        더하기 : add(x) // update member set age = age + x
//        곱하기 : multiply(x) // update member set age = age * x
    }

    /*벌크연산 - 삭제 */
    @Test
    public void bulkDelete() throws Exception{
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * SQL function 호출하기
     * SQL function은 JPA와 같이 Dialect에 등록된 내용만 호출할 수 있다.
     * ex) H2사용시 H2Dialect.java에 명시되어있는 함수만 사용가능. 사용자 설정 가능
     */
    @Test
    public void sqlFunction() throws Exception{
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username,"member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 소문자로 변경해서 비교해라.
     */
    @Test
    public void sqlFunction2() throws Exception{
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(  // lower 같은 ansi 표준 함수들은 querydsl이 상당부분 내장하고 아래와 같이 변경 가능.
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }







}



