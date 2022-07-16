package study.querydsl.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

public interface MemberRepositoryCustom {
	
	List<MemberTeamDto> search(MemberSearchCondition condition);
	
	Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
	
	Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);

	// page로 반환하면, 페이지 개수/ 총 개수/ first인지 last인지/ 비어있는지 등 담겨서 나옴
}
