package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired EntityManager em;
    @Autowired MemberRepository memberRepository;

    @Test
    public void basicTest() throws Exception{
        //given
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        //then
        Member findMember = memberRepository.findById(member.getId()).get();// 원래 Optional 반환값을 그대로 .get()하면 안되지만, 테스트이므로 그냥 허용
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll(); // findAll_Querydsl();로 바꿔서 테스트
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

    /**
     * 사용자 정의 리포지토리에서 정의한 동적쿼리 테스트
     */
    @Test
    public void searchTest() throws Exception{

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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
//        condition.setUsername("3");

        List<MemberTeamDto> result = memberRepository.search(condition);

        for (MemberTeamDto member : result) {
            System.out.println("member = " + member);
        }
        assertThat(result).extracting("username").containsExactly("member4");
    }


    @Test
    public void searchPageSimpleTest() throws Exception{
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

        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);  // 스프링 데이터 페이지는 인덱스가 0부터 시작.  0부터 총 3개를 가져온다. 0, 1, 2 까지.

        Page<MemberTeamDto> result = memberRepository.searchPage(condition, pageRequest);

        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

    /**
     * 스프링데이터JPA의 QuerydslPredicateExecutor의 한계점.
     * 조인X (묵시적 조인은 가능하지만 left join이 불가능하다.)
     * 클라이언트가 Querydsl에 의존해야 한다. 서비스 클래스가 Querydsl이라는 구현 기술에 의존해야 한다.
     * └> 이 테스트코드가 controller 혹은 service 레이어의 역할을 한다.
     * 그런데 코드를 보면 여기서 QMember를 생성해서 리포지토리를 호출한다.
     * Repository를 만들어 쓰는 이유는 그 하부에 Querydsl같은 구체화된 기술을 숨기는 용도인데, 이 경우 레이어 아키텍쳐를 위배하게 된다.
     */
    @Test
    public void querydslPredicateExecutorTest() throws Exception{
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

        QMember member = QMember.member;
        Iterable<Member> result = memberRepository.findAll(member.age.between(10, 40).and(member.username.eq("member1")));

        for (Member findMember : result) {
            System.out.println("findMember = " + findMember);
        }
    }



}