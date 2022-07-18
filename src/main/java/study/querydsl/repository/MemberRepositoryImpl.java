package study.querydsl.repository;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.util.StringUtils;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

public class MemberRepositoryImpl implements MemberRepositoryCustom{
	// extends QuerydslRepositorySupport
	
//	public MemberRepositoryImpl(Class<?> domainClass) {
//		super(Member.class);
//	}
	
	private final JPAQueryFactory query;
	
	public MemberRepositoryImpl(EntityManager em) {
		this.query = new JPAQueryFactory(em);
	}
	
	@Override
	public List<MemberTeamDto> search(MemberSearchCondition condition){
		
		// QuerydslRepositorySupport 의 지원을 받으면 아래 형식으로 사용
		// sort 가 정상 작동하지 않는다는 치명적인 단점때문에 사용 x
//		from(member)
//		.leftJoin(member.team, team)
//		.where(
//				usernameEq(condition.getUsername()),
//				teamNameEq(condition.getTeamName()),
//				ageGoe(condition.getAgeGoe()),
//				ageLoe(condition.getAgeLoe())
//				)
//		.select(new QMemberTeamDto(
//				member.id.as("memberId"), // MemberTeamDto 에서 받을 필드명을 as로
//				member.username,
//				member.age,
//				team.id.as("teamId"),
//				team.name.as("teamName")
//			))
//		.fetch();
		
		return query
				.select(new QMemberTeamDto(
							member.id.as("memberId"), // MemberTeamDto 에서 받을 필드명을 as로
							member.username,
							member.age,
							team.id.as("teamId"),
							team.name.as("teamName")
						))
				.from(member)
				.leftJoin(member.team, team)
				.where(
						usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoe(condition.getAgeGoe()),
						ageLoe(condition.getAgeLoe())
						)
				.fetch();
	}
	
	private BooleanExpression usernameEq(String username) {
		return StringUtils.hasText(username) ? member.username.eq(username) : null;
	}
	private BooleanExpression teamNameEq(String teamName) {
		return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
	}
	private BooleanExpression ageGoe(Integer ageGoe) {
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}
	private BooleanExpression ageLoe(Integer ageLoe) {
		return ageLoe != null ? member.age.loe(ageLoe) : null;
	}
	
	// 얘는 이제 못씀
	@Override
	public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
		QueryResults<MemberTeamDto> result = query
				.select(new QMemberTeamDto(
							member.id.as("memberId"), // MemberTeamDto 에서 받을 필드명을 as로
							member.username,
							member.age,
							team.id.as("teamId"),
							team.name.as("teamName")
						))
				.from(member)
				.leftJoin(member.team, team)
				.where(
						usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoe(condition.getAgeGoe()),
						ageLoe(condition.getAgeLoe())
						)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetchResults(); // count 쿼리까지 날림
		
		List<MemberTeamDto> content = result.getResults();
		long total = result.getTotal();
		
		return new PageImpl<>(content, pageable, total);
	}
	
	// TODO : querydsl 에서 pageable이용
	// fetchResults 사용하지 않는 쿼리
	@Override
	public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
		List<MemberTeamDto> result = query
				.select(new QMemberTeamDto(
							member.id.as("memberId"), // MemberTeamDto 에서 받을 필드명을 as로
							member.username,
							member.age,
							team.id.as("teamId"),
							team.name.as("teamName")
						))
				.from(member)
				.leftJoin(member.team, team)
				.where(
						usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoe(condition.getAgeGoe()),
						ageLoe(condition.getAgeLoe())
						)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch(); // count 쿼리까지 날림
		
		long total1 = query
			.select(member)
			.from(member)
			.leftJoin(member.team, team)
			.where(
					usernameEq(condition.getUsername()),
					teamNameEq(condition.getTeamName()),
					ageGoe(condition.getAgeGoe()),
					ageLoe(condition.getAgeLoe())
					)
			.fetchCount(); // 이것도 이제 못씀
		
		// 이렇게 count 쿼리를 만드는 방법도 있지만, 
		List<Long> totalCount = query
				.select(member.count())
				.from(member)
				.leftJoin(member.team, team)
				.where(
						usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoe(condition.getAgeGoe()),
						ageLoe(condition.getAgeLoe())
						)
				.fetch();
		System.out.println("totalCount : " + totalCount.get(0));
		
		// 자바단에서 size() 이용하는게 낫다
		long total = result.size();
			
		//return new PageImpl<>(result, pageable, total);
		return new PageImpl<>(result);
	}
	// + sort도 querydsl이 지원해주지만 조건이 조금만 복잡해져도 사용하기 어려워지기 때문에
	// 루트 엔티티를 벗어나서 동적 정렬이 필요하다면 파라미터를 직접 받아서 처리하는게 낫다
	
}
