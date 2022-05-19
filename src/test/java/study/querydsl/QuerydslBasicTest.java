package study.querydsl;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public  void before() {
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
    public void startJPQL() throws Exception{
        //member1을 찾아라.
        String qlString = "select m from Member m" +
                " where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)  // ctrl + alt + V 로 변수 추출
                .setParameter("username", "member1")
                .getSingleResult();  //결과가 하나일 때 사용. 없거나 한개 이상이면 Exception 발생

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuesydsl() throws Exception{
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);  // 1. JPAQueryFactory 생성 .. 필드로 빼내고, beforeg함수에서 생성
        QMember m = new QMember("m");  // 2. Qmember 생성. // 실무에선 이미 만들어져 있는 QMember member를 사용함.

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))  // Querydsl이 JDBC prepareStatement 로 자동으로 파라미터 바인딩 해준다.
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


}
