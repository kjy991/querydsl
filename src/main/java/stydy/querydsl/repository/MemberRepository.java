package stydy.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.StringUtils;
import stydy.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    List<Member> findByUsername(String username);
}
