package study.querydsl;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
	
	@Autowired
	EntityManager em;
	
	JPAQueryFactory queryFactory;
	
	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);
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
	public void startJPQL() {
		
		// member1 찾기
		Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
		.setParameter("username", "member1")
		.getSingleResult();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void startQuerydsl() {
		// TODO: 새로 엔티티를 만들었다면, 이클립스 + gradle 기준으로는
		// 다시 build를 해준뒤 gradle refresh해야 build/generated/querydsl (설정한 경로)
		// 에 QClass 가 적용된다
		
		//QMember m = QMember.member;
		
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("member1")) // 파라미터 바인딩
			.fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void search() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1")
					.and(member.age.eq(10)))
				.fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
		// eq 뿐만아니라
		// in, notIn, between, goe, gt, loe, lt, like, contatins, startsWith 등 쓸 수 있음
	}
	
	// .and 를 사용하는 대신 ','로 조건을 이어줄 수 있음
	@Test
	public void searchAndParam() {
		Member findMember = queryFactory
				.selectFrom(member)
				.where(member.username.eq("member1"),
						member.age.eq(10)
						)
				.fetchOne();
		
		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}
}
