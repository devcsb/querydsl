package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local") // 프로파일 설정. 부트가 처음 뜰 때 둘째 줄 로그에서 현재 사용중인 profile 확인 가능
@Component //스프링 빈에 자동 등록 되도록 설정
@RequiredArgsConstructor
public class InitMember {
    private final InitMemberService initMemberService;

    @PostConstruct  // @PostConstruct 는 WAS 가 뜰 때 bean이 생성된 다음 딱 한번만 실행된다.
    public void init() {
        /*스프링 라이프사이클 때문에, PostConstruct 와 Transactional을 동시에 쓸 수 없으므로, 분리하여 호출한다.*/
        initMemberService.init();
    }

    @Component
    static class InitMemberService {

        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member" + i, i, selectedTeam));
            }

        }
    }

}
