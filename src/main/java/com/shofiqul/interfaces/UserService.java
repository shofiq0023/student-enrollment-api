package com.shofiqul.interfaces;

import org.springframework.http.ResponseEntity;

import com.shofiqul.dto.UserAuthReq;
import com.shofiqul.dto.UserRegisterReq;
import com.shofiqul.dto.UserUpdateReq;

public interface UserService {

	ResponseEntity<?> userAuthenticate(UserAuthReq req);

	ResponseEntity<?> userRegister(UserRegisterReq req, String role);

	ResponseEntity<?> getUserDetails(long userId);

	ResponseEntity<?> getAllUsers();

	ResponseEntity<?> updateUser(UserUpdateReq reqDto);

	ResponseEntity<?> deleteUser(long userId);
}
