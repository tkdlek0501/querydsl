package study.querydsl.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
//@RequiredArgsConstructor
public class MemberJpaRepository {
	
	
	private final EntityManager em;
	private final JPAQueryFactory queryFactory;
	
	public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
		this.em = em;;
		this.queryFactory = queryFactory;
		//this.queryFactory = new JPAQueryFactory(em);
	}
	
	public void save(Member member) {
		em.persist(member);
	}
	
	public Optional<Member> findById(Long id){
		Member findMember = em.find(Member.class, id);
		return Optional.ofNullable(findMember);
	}
	
	public List<Member> findByUsername(String username){
		return em.createQuery("select m from Member m where m.username = :username", Member.class)
				.setParameter("username", username)
				.getResultList();
	}
	
	public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
		
		BooleanBuilder builder = new BooleanBuilder();
		if(StringUtils.hasText(condition.getUsername())) { // not null + not "" 
			builder.and(member.username.eq(condition.getUsername()));
		}
		if(StringUtils.hasText(condition.getTeamName())) {
			builder.and(team.name.eq(condition.getTeamName()));
		}
		if(condition.getAgeGoe() != null) {
			builder.and(member.age.goe(condition.getAgeGoe()));
		}
		if(condition.getAgeLoe() != null) {
			builder.and(member.age.loe(condition.getAgeLoe()));
		}
		
		return queryFactory
				.select(new QMemberTeamDto(
							member.id.as("memberId"), // MemberTeamDto 에서 받을 필드명을 as로
							member.username,
							member.age,
							team.id.as("teamId"),
							team.name.as("teamName")
						))
				.from(member)
				.leftJoin(member.team, team)
				//.on(builder) -> 안됨
				.where(builder)
				.fetch();
		
		// where 절에 조건이 안들어가면 전체를 조회하게 된다
		// DB에 raw 수가 많다면 paging을 해야 한다
	}
}
