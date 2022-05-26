package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.function.Supplier;

import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class DynamicQueryTest {

    @Autowired EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    void init() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        em.persist(new Member("memberA", 10, teamA));
        em.persist(new Member("memberB", 20, teamA));
        em.persist(new Member("memberC", 30, teamB));
        em.persist(new Member("userA", 30, teamA));
        em.persist(new Member("userB", 10, teamB));
    }

    /**
     * 정책 : 이름에 member를 포함, 나이가 10살 이상, team 이름에 team을 포함하는 회원만 유효한 회원이라고 가정,
     * 유효한 모든 회원을 가져오기.
     */
    @Test
    void 동적쿼리_테스트() {
        String username = "member";
        Integer age = 10;
        String teamName = "team";

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(validMember(username, age, teamName)
                )
                .fetch();

        System.out.println("****************************");
        for (Member member : result) {
            System.out.println("result = " + member);
        }
        System.out.println("****************************");
    }

    private BooleanBuilder validMember(String username, Integer age, String teamName) {
        return validName(username).and(ageGoe(age)).and(validTeam(teamName));
    }

    private BooleanBuilder validName(String username) {
        if (username == null) {
            return new BooleanBuilder();
        } else {
            return new BooleanBuilder(member.username.contains(username));
        }
    }

    private BooleanBuilder ageGoe(Integer age) {
        if (age == null) {
            return new BooleanBuilder();
        } else {
            return new BooleanBuilder(member.age.goe(age));
        }
    }

    private BooleanBuilder validTeam(String teamName) {
        if (teamName == null) {
            return new BooleanBuilder();
        } else {
            return new BooleanBuilder(member.team.name.contains(teamName));
        }
    }

    /**
     * 정책 : username이 memberC이고, 나이가 30살이며, team이름이 정확히 teamB인 회원만 가져오기
     */
    @Test
    void 동적쿼리_테스트2_람다활용() {

        String username = "memberC";
        Integer age = null;
        String teamName = "teamB";

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(validMember2(username, age, teamName)
                )
                .fetch();

        System.out.println("****************************");
        for (Member member : result) {
            System.out.println("result = " + member);
        }
        System.out.println("****************************");
    }

    private BooleanBuilder validMember2(String username, Integer age, String teamName) {
        return nameEq(username).and(ageEq(age)).and(teamEq(teamName));
    }

    private BooleanBuilder nameEq(String username) {
        return nullSafeBuilder(() -> member.username.eq(username));
    }

    private BooleanBuilder ageEq(Integer age) {
        return nullSafeBuilder(() -> member.age.eq(age));
    }

    private BooleanBuilder teamEq(String teamName) {
        return nullSafeBuilder(() -> member.team.name.eq(teamName)); //(매개변수, ...) -> { 실행문 ... }
    }

    /* Supplier : 인자를 받지 않고 Type T 객체를 리턴하는 함수형 인터페이스 T.get()으로 값 반환*/
    public static BooleanBuilder nullSafeBuilder(Supplier<BooleanExpression> f) {
        try {
            return new BooleanBuilder(f.get());
        } catch (IllegalArgumentException e) {
            return new BooleanBuilder();
        }
    }


}
