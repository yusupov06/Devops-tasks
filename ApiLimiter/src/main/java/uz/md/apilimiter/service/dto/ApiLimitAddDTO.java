package uz.md.apilimiter.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApiLimitAddDTO {

    private String apiRegex;

    private Long limitCount;

    private int priority;

    private Long tariffId;
}
