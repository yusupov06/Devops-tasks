package uz.md.synccachereactive.mapper;

import org.mapstruct.Mapper;
import uz.md.synccachereactive.dtos.TransactionDTO;
import uz.md.synccachereactive.entity.Transaction;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionDTO toDTO(Transaction transaction);

    List<TransactionDTO> toDTO(List<Transaction> currencies);
}
