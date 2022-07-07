package study.querydsl;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.Tuple;
import com.querydsl.core.annotations.QueryProjection;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

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

// TODO: querydsl 문법	
	
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
	
//	@Test
	public void resultFetch() {
		List<Member> fetch = queryFactory
			.selectFrom(member)
			.fetch();
		
		Member fetchOne = queryFactory
				.selectFrom(member)
				.fetchOne();
		
		Member fetchFirst = queryFactory
				.selectFrom(member)
				.fetchFirst();

// fetchResults(), fetchCount() 는 querydsl 에서 앞으로 지원 안함 
//		-> groupby 나 having 절 포함하는 복잡한 쿼리시 부정확한 결과가 나오기 때문 + 성능 문제
//		=> count를 세려면 가져와서 size() 로 구해야 한다
//		QueryResults<Member> results = queryFactory
//				.selectFrom(member)
//				.fetchResults(); 
		
//		results.getTotal();
//		List<Member> content = results.getResults();
	}
	
	// 정렬
	// 나이 내림차순 desc
	// 이름 올림차순 asc 
	// 단, 이름이 없으면 마지막에 출력되도록 nulls last
	@Test
	public void sort() {
		em.persist(new Member(null, 100)); // 이름 없는 회원 
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));
		
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();
		
		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);
		Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
		Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
		Assertions.assertThat(memberNull.getUsername()).isNull();
	}
	
	@Test
	public void paging1() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();
		
		Assertions.assertThat(result.size()).isEqualTo(2);
	}
	
	// 집합 (tuple 사용)
	@Test
	public void aggregation() {
		List<Tuple> result = queryFactory
			.select(
					member.count(),
					member.age.sum(),
					member.age.avg(),
					member.age.max(),
					member.age.min()
					)
			.from(member)
			.fetch();
		
		Tuple tuple = result.get(0);
		
		Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
		Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
		Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}
	
	//팀의 이름과 각 팀의 평균 연령을 구해라
//	@Test
	public void group() throws Exception {
		List<Tuple> result = queryFactory
			.select(team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			// .having(item.price.gt(1000)) // having 절 예시
			.fetch();
			
		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);
		
		Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
		Assertions.assertThat(member.age.avg()).isEqualTo(15); // (10 + 20) / 2
		
		Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
		Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
		
	}
	
	// join은 첫 번째 파라미터에 조인 대상 지정, 두 번째 파라미터에 별칭으로 사용할 Q타입 지정
	@Test
	public void join() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team) // leftJoin 도 가능
			.where(team.name.eq("teamA"))
			.fetch();
		
		
		Assertions.assertThat(result)
				.extracting("username")
				.containsExactly("member1", "member2");
	}
	
	// 연관관계 없이 join 가능 - theta 사용 ; 외부 조인 불가능
	@Test
	public void theta_join() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		
		// member의 이름과 team 이름이 같은 것을 조회 (억지 예제)
		
		List<Member> result = queryFactory
					.select(member)
					.from(member, team) // 두 엔티티가 cross join 하게 된다
					.where(member.username.eq(team.name))
					.fetch();
		
		Assertions.assertThat(result)
		.extracting("username")
		.containsExactly("teamA", "teamB");
	}
	
	// 연관관계 없을 때 외부 조인까지 사용하는 방법 - on 사용
	// on절 활용 : 1. 조인 대상 필터링, 2. 연관관계 없는 엔티티 외부 조인
	// 1. 조인 대상 필터링
	@Test
	public void join_on_filtering() { // 회원과 팀 조인 + 팀이름이 teamA인 팀만 조인
		
		// jpql : select m, t from Member m left join m.team t on t.name = "teamA"
		List<Tuple> result = queryFactory // select의 결과가 여러 타입으로 반환되므로 Tuple
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team).on(team.name.eq("teamA"))
			//.join(member.team, team) // innerJoin 일떄는
			//.where(team.name.eq("teamA")) // on절은 where절로 써도 결과 동일하다
			.fetch();
		
		// *결론: innerJoin 일때는 where 절을 사용하고,
		// 	outer join 이면 on 절을 사용하면 된다. (where절 사용하면 outerJoin의 결과와 다르게 회원도 안나온다)
		// 근데 웬만하면 innerJoin을 사용해서 필터링을 하지 굳이 필터링으로 제외한 대상까지 가져오지 않음
		
		for(Tuple tuple : result) {
			System.out.println("tuple = " + tuple); // teamB인 것은 회원 data는 가져오지만 team은 null이 됨
		}
	}
	// 2. 연관관계 없는 엔티티 외부 조인 (join시 on절에 id가 아닌 다른 컬럼 조건으로 가져올 때)
	@Test
	public void join_on_no_relation() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));
		
		// member의 이름과 team 이름이 같은 것을 조회 (억지 예제)
		
		List<Tuple> result = queryFactory
					.select(member, team)
					.from(member)
					.leftJoin(team) // 보통 (member.team, team) 이렇게 join을 하지만, 그냥 join할 대상만 지정하고
					// .join(team)
					.on(member.username.eq(team.name)) // 이렇게 on 절만 사용하면 id 동일 조건을 제외하고 이 조건만으로 조회
					// 즉, join(member.team, team) 이렇게 조인할 엔티티를 둘다 지정하면 연관관계로 인해 on절에 id 조건이 자동으로 생성되었던 것?
					.fetch();
		
		for(Tuple tuple : result) {
			System.out.println("tuple : " + tuple);
		}
	}
	
	
	@PersistenceUnit
	EntityManagerFactory emf;
	
	// fetch join 하는 법 (단, 1:N 조회는 fetch size설정해서 lazy loading으로 가져와야 한다)
	@Test
	public void fetchJoinNo() {
		em.flush();
		em.clear();
		
		Member findMember = queryFactory
				.selectFrom(member)
				.where(member.username.eq("member1"))
				.fetchOne();
		// member만 조회하는 쿼리 ; team은 초기화 전이라 get했을 때 lazy loading으로 조회됨
		
		Boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화(로딩)가 됐는지 안됐는지 여부 반환
		Assertions.assertThat(loaded).isFalse();
	}
	
	// fetch join 사용
	@Test
	public void fetchJoinUse() {
		em.flush();
		em.clear();
		
		Member findMember = queryFactory
				.selectFrom(member)
				.join(member.team, team).fetchJoin()
				.where(member.username.eq("member1"))
				.fetchOne();
		// member 조회시 team 까지 join해서 동시에 가져오는 쿼리
		
		Boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); // 초기화(로딩)가 됐는지 안됐는지 여부 반환
		Assertions.assertThat(loaded).isTrue();
	}
	
	// sub query
	@Test
	public void subQuery() {
		// 나이가 가장 많은 회원 조회
		
		QMember memberSub = new QMember("memberSub"); // sub query에서 사용
		
		List<Member> result = queryFactory	
			.selectFrom(member)
			.where(member.age.eq(
						JPAExpressions
							.select(memberSub.age.max())
							.from(memberSub)
					))
			.fetch();
		
		Assertions.assertThat(result).extracting("age")
			.containsExactly(40);
	}
	
	@Test
	public void subQueryGoe() {
		// 나이가 평균 이상인 회원
		
		QMember memberSub = new QMember("memberSub"); // sub query에서 사용
		
		List<Member> result = queryFactory	
			.selectFrom(member)
			.where(member.age.goe(
						JPAExpressions
							.select(memberSub.age.avg())
							.from(memberSub)
					))
			.fetch();
		
		Assertions.assertThat(result).extracting("age")
			.containsExactly(30, 40);
	}
	
	@Test
	public void subQueryIn() {
		// 나이가 10세 이상인 회원 조회 (굳이 in절 쓸 필요 없지만 예제상)
		
		QMember memberSub = new QMember("memberSub"); // sub query에서 사용
		
		List<Member> result = queryFactory	
			.selectFrom(member)
			.where(member.age.in(
						JPAExpressions
							.select(memberSub.age)
							.from(memberSub)
							.where(memberSub.age.gt(10))
					))
			.fetch();
		
		Assertions.assertThat(result).extracting("age")
			.containsExactly(20, 30, 40);
	}
	
	@Test
	public void selectSubQuery() {
		
		// username 과 평균 나이 조회
		
		QMember memberSub = new QMember("memberSub");
		
		List<Tuple> result = queryFactory
			.select(member.username,
					JPAExpressions // static import 가능
					.select(memberSub.age.avg())
					.from(memberSub)
					)
			.from(member)
			.fetch();
		
		for(Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}
	
	// * from 절에서 서브쿼리는 사용할 수 없음(한계)
	// JPQL의 한계점으로 QueryDSL도 당연히 지원 안된다
	
	// 해결방안
	// 1. 서브쿼리를 가능하다면 join으로 변경한다. (일반적으로 가능한 경우가 많다)
	// 2. 애플리케이션에서 쿼리를 2개로 분리해서 실행한다. (성능 문제가 없다면 이 방법 사용)
	// 3. nativeSQL을 사용한다. (안되면 @Query로 nativeSQL 사용하면 된다)
	
	// case 문
	@Test
	public void basicCase() {
		List<String> result = queryFactory
			.select(member.age
					.when(10).then("열살")
					.when(20).then("스무살")
					.otherwise("기타")
					)
			.from(member)
			.fetch();
		
		for(String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	@Test
	public void complexCase() {
		List<String> result = queryFactory
			.select(new CaseBuilder() // when절에 조건을 추가하려면 CaseBuilder() 필요
					.when(member.age.between(0, 20)).then("0~20살")
					.when(member.age.between(21, 30)).then("21~30살")
					.otherwise("기타")
					)
			.from(member)
			.fetch();
		
		for(String s : result) {
			System.out.println("s = " + s);
		}
		
		// 이런 복잡한 조건이 있는 경우에는 애플리케이션에서 로직을 만들어 처리하는 게 낫다
		// DB에서는 필요한 data만을 조회하기 위한 쿼리를 작성해야지 data 가공을 하는 것은 지양하는 게 좋다
	}
	
	// 상수, 문자 더하기 -> 이것도 data를 가공해서 추출하는 방법이니까 지양해야 하지 않을까? 애플리케이션에서 충분히 처리 가능
	@Test
	public void constant() {
		List<Tuple> result = queryFactory
			.select(member.username, Expressions.constant("A"))
			.from(member)
			.fetch();
			
			for(Tuple tuple : result) {
				System.out.println("tuple = " + tuple);
			}
	}
	
	@Test
	public void concat() {
		List<String> result = queryFactory
			.select(member.username.concat("_").concat(member.age.stringValue()))
			// .stringValue() : age는 숫자 이므로 string으로 변환해야 한다 -> 특히 Enum을 처리할 때 자주 사용 (ex. member안에 enum을 쿼리에 넣고 싶을 때)
			.from(member)
			.where(member.username.eq("member1"))
			.fetch();
		
		for(String s : result) {
			System.out.println("s = " + s);
		}
	}
	
	// 프로젝션 : select 대상 지정
	// 대상이 하나이면 타입을 명확하게 할 수 있지만
	// 대상이 여러개이면 tuple or dto로 조회해야 한다
	
	@Test
	public void simpleProjection() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();
		
		for(String r : result) {
			System.out.println("r : " + r);
		}
	}
	
	@Test
	public void tupleProjection() {
		List<Tuple> result = queryFactory
				.select(member.username, member.age)
				.from(member)
				.fetch();
		
		for(Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);
			System.out.println("username : " + username);
			System.out.println("age : " + age);
		}
	}
	// tuple은 레포지토리 계층에서 쓰는건 괜찮지만, 서비스계층 이상으로 넘어가는 것은 좋지 않다
	// 핵심 비즈니스 로직에서 레포지토리에서 어떤 기술을 사용하는지 아는 것과 그것을 의존하는 것은 좋지 않음
	
	// JPQL 로 DTO 조회하는 방법
	@Test
	public void findDtoByJPQL() {
		List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
		.getResultList();
		
		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}
	// 생성자 방식만 지원하고 이를 package이름으로 적어줘야 해서 지저분하다
	
	// TODO: dto 조회
	// querydsl은 3가지 방법을 지원한다
	// 프로퍼티 접근, 필드 직접 접근, 생성자 사용
	
	// 1. 프로퍼티 접근 - setter
	// *setter를 만들어야 한다
	@Test
	public void findDtoBySetter() {
		List<MemberDto> result = queryFactory
			.select(Projections.bean(MemberDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
		
		for(MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}
	
	// 2. 필드 직접 접근 (setter 안 만들어도 됨)
	@Test
	public void findDtoByField() {
		// QMember memberSub = new QMember("memberSub");
		QMember memberSub = QMember.member;
		List<MemberDto> result = queryFactory
			.select(Projections.fields(MemberDto.class,
					member.username.as("username"), // *만약 조회할 DTO와 엔티티의 필드명이 다른 경우에는 as로 DTO의 필드명을 지정하면 된다
					// member.age,
					ExpressionUtils.as(JPAExpressions   // subquery
							.select(memberSub.age.max())
							.from(memberSub), "age"
							)
					))
			.from(member)
			.fetch();
		
		for(MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}
	
	// 3. 생성자 방식 (만든 생성자와 타입을 맞춰줘야 한다, 필드명은 안맞춰줘도 된다.(매개변수로 들어가는 거니까))
	@Test
	public void findDtoByConstructor() {
		List<MemberDto> result = queryFactory
			.select(Projections.constructor(MemberDto.class,
					member.username,
					member.age))
			.from(member)
			.fetch();
		
		for(MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}
	
	// TODO: dto 조회 (dto도 QClass로 만들기)
	// MemberDto 의 생성자에 @QueryProjection 붙이고 build 뒤에 gradle을 refresh 하면 QClass 생성된다
	// 인텔리제이에서는 compileQuerydsl
	@Test
	public void findDtoByQueryProjection() {
		List<MemberDto> result = queryFactory
			.select(new QMemberDto(member.username, member.age))
			.from(member)
			.fetch();
		
		for(MemberDto memberDto : result) {
			System.out.println("memberDto : " + memberDto);
		}
	}
	// 생성자 방식과의 차이는 생성자의 파라미터를 잘못 입력했을 때, 런타임 오류를 내느냐 컴파일 오류를 내느냐
	// 문제점은 DTO가 @QueryProjection을 쓰면서 querydsl 에 의존을 가지게 된다는 것 (+ QClass를 생성해줘야 한다는 것)
	// 아키텍쳐를 비교적 잘 설계 하느냐 vs 런타임 오류를 막느냐
	// 개인적으로 생성자 방식을 사용하는게 좋아보인다... 
}
