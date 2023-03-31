package ru.kpfu.itis.service.impl;

import org.springframework.mail.javamail.JavaMailSender;
import ru.kpfu.itis.config.MailConfig;
import ru.kpfu.itis.dto.CreateUserRequestDto;
import ru.kpfu.itis.dto.UserResponseDto;
import ru.kpfu.itis.Model.User;
import ru.kpfu.itis.repository.UserRepository;
import ru.kpfu.itis.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import net.bytebuddy.utility.RandomString;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final JavaMailSender javaMailSender;
    private final MailConfig mailConfig;

    public UserResponseDto getByEmail(String email) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUserName = authentication.getName();
        }
        return userRepository.getUserByEmail(email).stream().map(UserResponseDto::fromEntity).findFirst().orElse(null);
    }

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
    public UserResponseDto create(CreateUserRequestDto createUserDto, String url) {
        String code = RandomString.make(64);
        String encodedPassword = encoder.encode(createUserDto.getPassword());
        User user = User.builder()
                .name(createUserDto.getName())
                .email(createUserDto.getEmail())
                .verificationCode(code)
                .password(encodedPassword)
                .build();
        sendVerificationMail(createUserDto.getEmail(), createUserDto.getName(), code, url);
        return UserResponseDto.fromEntity(user);
    }

    @Override
    public boolean verify(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);
        if (user != null) {
            user.setVerificationCode(null);
            user.setEnabled(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public void sendVerificationMail(String mail, String name, String code, String url) {
        String from = mailConfig.getFrom();
        String sender = mailConfig.getSender();
        String subject = mailConfig.getSubject();
        String content = mailConfig.getContent();


        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        try {
            helper.setFrom(from, sender);

            helper.setTo(mail);
            helper.setSubject(subject);

            content = content.replace("{name}", name);
            content = content.replace("{url}", url + "/verification?code=" + code);

            helper.setText(content, true);
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        javaMailSender.send(mimeMessage);
    }
}