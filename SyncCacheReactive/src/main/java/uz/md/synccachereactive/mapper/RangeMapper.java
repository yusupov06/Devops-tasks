package uz.md.synccachereactive.mapper;

import org.mapstruct.Mapper;
import uz.md.synccachereactive.dtos.RangeDTO;
import uz.md.synccachereactive.entity.Range;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RangeMapper {

    RangeDTO toDTO(Range range);

    List<RangeDTO> toDTO(List<Range> ranges);
}
