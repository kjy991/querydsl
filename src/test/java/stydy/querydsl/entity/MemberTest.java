package stydy.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import stydy.querydsl.dto.MemberDto;
import stydy.querydsl.dto.QMemberDto;
import stydy.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.core.types.Projections.*;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static stydy.querydsl.entity.QMember.*;
import static stydy.querydsl.entity.QTeam.*;


@SpringBootTest
@Transactional
@Rollback(value = false)
class MemberTest {
    JPAQueryFactory queryFactory;

    @Autowired
    EntityManager em;

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
    public void startJPAL() {
        // member1을 찾아라
        Member findMember = em.createQuery("select m " +
                        "from Member m " +
                        "where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {

        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFtect() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//        Member fetchOne = queryFactory.selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchOne();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//        results.getTotal();
//        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.eq(100)
                )
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> q = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(q.getTotal()).isEqualTo(4);
        assertThat(q.getLimit()).isEqualTo(2);
        assertThat(q.getOffset()).isEqualTo(1);
        assertThat(q.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void group() throws Exception {

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void join() throws Exception {
        List<Member> teamA = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(teamA)
                .extracting("username")
                .containsExactly("member1", "member2");
    }


    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> fetch = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        assertThat(fetch)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .innerJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30, 40);

    }

    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);

    }

    @Test
    public void selectSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() throws Exception {
        List<String> fetch = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() throws Exception {
        List<String> 기타 = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : 기타) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() throws Exception {
        List<Tuple> a = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : a) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() throws Exception {
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() throws Exception {
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String name = tuple.get(member.username);
            System.out.println("name = " + name);
            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJpql() throws Exception {
        List<MemberDto> resultList = em.createQuery(
                        "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                                "from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {

        List<MemberDto> fetch = queryFactory
                .select(bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() {

        List<MemberDto> fetch = queryFactory
                .select(fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> fetch = queryFactory
                .select(constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> fetch = queryFactory
                .select(constructor(UserDto.class,
                        member.username.as("name"),
//                        member.age.as("age"),
                        ExpressionUtils.as(
                                select(memberSub.age.max())
                                        .from(memberSub), "age"
                        )
                ))
                .from(member)
                .fetch();
        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);

        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder();

        if (usernameParam != null) {
            builder.and(member.username.eq(usernameParam));
        }

        if (ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .select(member)
                .from(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .select(member)
                .from(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }


}
























