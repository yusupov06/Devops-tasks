package uz.md.apilimiter.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.*;
import org.apache.catalina.User;
import uz.md.apilimiter.domain.templ.AbsLongEntity;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@ToString
public class UserLimit extends AbsLongEntity {

    @ManyToOne
    private ApiLimit apiLimit;

    private String username;

    private Long limitCount;

    private Instant activeFrom;

    private Instant activeTo;

    private boolean active;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserLimit) {
            UserLimit userLimit = (UserLimit) obj;
            return this.getId().equals(userLimit.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
