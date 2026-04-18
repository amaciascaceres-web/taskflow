package com.taskflow.user.infrastructure.repository;

import com.taskflow.user.infrastructure.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
