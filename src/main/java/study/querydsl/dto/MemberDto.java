package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 기본생성자는 안만들어준다.
@NoArgsConstructor // query Projection 사용시, setter, field 방식일 때는 기본생성자로 먼저 Dto를 만들고 접근하기 때문에, 기본생성자가 필요하다.
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection  // ./gradlew compileQuerydsl  QMemberDto라는 dto의 q파일이 생성된다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
