package ru.kpfu.itis.service.impl;

import ru.kpfu.itis.dto.CreateUserRequestDto;
import ru.kpfu.itis.dto.UserResponseDto;
import ru.kpfu.itis.Model.User;
import ru.kpfu.itis.repository.UserRepository;
import ru.kpfu.itis.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    @Override
    public List<UserResponseDto> findAll() {
        return userRepository.findAll().stream().map(u -> UserResponseDto.builder()
                .name(u.getName())
                .id(u.getId())
                .email(u.getEmail())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public Optional<UserResponseDto> findById(Integer id) {
        return userRepository.findAllById(List.of(id))
                .stream().map(u -> UserResponseDto.builder()
                        .name(u.getName())
                        .id(u.getId())
                        .email(u.getEmail())
                        .build()
                ).findFirst();
    }

    @Override
    public UserResponseDto create(CreateUserRequestDto userDto) {
        User user = User.builder()
                .name(userDto.getName())
                .email(userDto.getEmail())
                .password(encoder.encode(userDto.getPassword()))
                .build();
        return UserResponseDto.fromEntity(userRepository.save(user));
    }
}
