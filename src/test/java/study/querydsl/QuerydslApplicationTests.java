package study.querydsl;

import javax.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;
	
	@Test
	void contextLoads() {
		// querydsl test
		Hello hello = new Hello();
		em.persist(hello);
		
		JPAQueryFactory query = new JPAQueryFactory(em);
		// QHello qHello = new QHello("h"); // qClass 에 alias 설정
		QHello qHello = QHello.hello; // 위에 코드를 new를 쓰지 않고 이렇게 쓸 수 있다
		
		// querydsl ; 여기서는 qType 을 넣어야 한다
		Hello result = query
			.selectFrom(qHello)
			.fetchOne();
		
		// 검증
		Assertions.assertThat(result).isEqualTo(hello);
		Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
	}

}
