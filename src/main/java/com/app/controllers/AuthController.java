package com.app.controllers;

import com.app.common.ERole;
import com.app.common.JwtUtils;
import com.app.configs.MessageResponse;
import com.app.dto.JwtResponse;
import com.app.dto.LoginRequest;
import com.app.dto.SignupRequest;
import com.app.entities.Role;
import com.app.entities.User;
import com.app.repotitories.RoleRepository;
import com.app.repotitories.UserRepository;
import com.app.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Validated @RequestBody LoginRequest loginRequest){
        //lây ra 1 đối tượng muốn được xác thực từ login request
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUserName(),loginRequest.getPassWord()));
       //thêm đối tượng này vào secutiry để xử lý tiếp
//        SecurityContextHolder.getContext().setAuthentication(authentication);
        //khởi tạo jwt từ đối tượng này
        String jwt = jwtUtils.generateJwtToken(authentication);
        //tạo đối tượng userdetail từ authen.getPrincipal
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        return ResponseEntity.ok(new JwtResponse(
                jwt,userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),roles
        ));

    }
    @PostMapping("/signup")
    public ResponseEntity<?> register(@Valid @RequestBody SignupRequest signUpForm){
        if(userRepository.existsByUserName(signUpForm.getUserName())){
            return new ResponseEntity<>(new MessageResponse("The username existed! Please try again!"), HttpStatus.OK);
        }
        if(userRepository.existsByEmail(signUpForm.getEmail())){
            return new ResponseEntity<>(new MessageResponse("The email existed! Please try again"), HttpStatus.OK);
        }
        User user = new User(signUpForm.getUserName(), signUpForm.getEmail(),passwordEncoder.encode(signUpForm.getPassWord()));
        Set<String> strRoles = signUpForm.getRole();
        Set<Role> roles = new HashSet<>();
        for (String role: strRoles
             ) {
            switch (role){
                case "admin":
                    Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(
                            ()-> new RuntimeException("Role not found")
                    );
                    roles.add(adminRole);
                    break;
                case "mod":
                    Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR).orElseThrow( ()-> new RuntimeException("Role not found"));
                    roles.add(modRole);
                    break;
                default:
                    Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow( ()-> new RuntimeException("Role not found"));
                    roles.add(userRole);
            }
        }
       ;
        user.setRoles(roles);
        userRepository.save(user);
        return new ResponseEntity<>(new MessageResponse("Create user success!"), HttpStatus.OK);
    }
}
