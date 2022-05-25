package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition { // 동적쿼리 생성에 사용할 멤버정보를 담은 Dto 객체
    //회원명, 팀명, 나이(ageGoe, ageLoe)
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
