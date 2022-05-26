package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

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
}