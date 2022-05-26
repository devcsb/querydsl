package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // query Projection 사용시, setter, field 방식일 때는 기본생성자로 먼저 Dto를 만들고 접근하기 때문에, 기본생성자가 필요하다.
public class UserDto {

    private String name;
    private int age;

    public UserDto(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
