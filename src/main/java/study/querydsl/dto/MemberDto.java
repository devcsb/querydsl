package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 기본생성자는 안만들어준다.
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection  // ./gradlew compileQuerydsl  QMemberDto라는 dto의 q파일이 생성된다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
