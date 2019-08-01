package com.code.auth.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class UserInfo {

    @Id
    private Long id;

    private String username;

    private String password;

    private String phone;
}
