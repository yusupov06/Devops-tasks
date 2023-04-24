//package uz.md.apilimiter.repository.impl;
//
//import org.springframework.stereotype.Repository;
//import uz.md.apilimiter.domain.UserLimit;
//import uz.md.apilimiter.repository.UserLimitRepository;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Repository
//public class UserLimitRepositoryImpl implements UserLimitRepository {
//
//    private final Set<UserLimit> USER_LIMITS = new HashSet<>();
//
//    @Override
//    public List<UserLimit> findAll() {
//        return new ArrayList<>(USER_LIMITS);
//    }
//
//    @Override
//    public UserLimit save(UserLimit userLimit) {
//        if (userLimit.getId()==null)
//            userLimit.setId((long) USER_LIMITS.size() + 1);
//        USER_LIMITS.add(userLimit);
//        return userLimit;
//    }
//
//    @Override
//    public void addAll(List<UserLimit> userLimits) {
//        USER_LIMITS.addAll(userLimits);
//    }
//
//    @Override
//    public Optional<UserLimit> findByUsernameAndLimit_Api(String username, String api) {
//        return USER_LIMITS.stream()
//                .filter(userLimit -> userLimit.getUsername().equals(username)
//                        && userLimit.getApiLimit().getApi().equals(api))
//                .findFirst();
//    }
//
//    @Override
//    public List<UserLimit> findAllByUsername(String username) {
//        return USER_LIMITS.stream()
//                .filter(userLimit -> userLimit.getUsername().equals(username))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public Optional<UserLimit> findById(Long userLimitId) {
//        return USER_LIMITS.stream()
//                .filter(userLimit -> userLimit.getId().equals(userLimitId))
//                .findFirst();
//    }
//}
