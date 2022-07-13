package study.querydsl.controller;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@Profile("local") // local로 설정한 profiles가 실행
@Component
@RequiredArgsConstructor
public class InitMember {
	
	private final InitMemberService initMemberService;
	
	@PostConstruct
	public void init() {
		initMemberService.init();
	}
	
	@Component
	static class InitMemberService {
		
		@PersistenceContext
		private EntityManager em;
		
		@Transactional
		public void init() {
			Team teamA = new Team("teamA");
			Team teamB = new Team("teawmB");
			em.persist(teamA);
			em.persist(teamB);
			
			for(int i = 0; i< 100; i++) {
				Team selectedTeam = i % 2 == 0 ? teamA : teamB;
				em.persist(new Member("member" + i, i, selectedTeam));
			}
		}
		
	}
	
}
