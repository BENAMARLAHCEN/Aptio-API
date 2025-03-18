package com.aptio.service;

import com.aptio.dto.PasswordUpdateDTO;
import com.aptio.dto.UserDTO;

import java.util.List;

public interface UserService {

    List<UserDTO> getAllUsers();

    UserDTO getUserById(String id);

    UserDTO getUserByEmail(String email);

    UserDTO getCurrentUser();

    UserDTO updateUser(String id, UserDTO userDTO);

    UserDTO updateCurrentUser(UserDTO userDTO);

    void changePassword(PasswordUpdateDTO passwordUpdateDTO);

    void resetUserPassword(String id, String newPassword);

    void deleteUser(String id);
}