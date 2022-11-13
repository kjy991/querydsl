package stydy.querydsl.repository;

import stydy.querydsl.dto.MemberSearchCondition;
import stydy.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom  {
    List<MemberTeamDto> search(MemberSearchCondition condition);

}
