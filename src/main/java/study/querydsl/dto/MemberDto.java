package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 기본생성자는 안만들어준다.
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
