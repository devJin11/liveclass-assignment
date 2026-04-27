package com.liveclass.assignment.domain.classmate.entity;


import com.liveclass.assignment.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *  과제의 요구사항에 인증/인가는 단순 파라미터로 처리하도록 되어 있으므로
 *  이 엔티티 클래스에 대한 CRUD api 설계 X.
 *  단순 DB 정합성 + FK 참조를 위해 만들었음.
 *  -> 테스트 데이터 생성은 seed.sql 데이터로만 생성하기 때문에 생성 메서드 만들지 않음.
 * */

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Classmate extends BaseEntity {

  public enum ClassmateStatus{
    ACTIVE,
    DELETED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "classmate_id", nullable = false)
  private Long id;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "email", nullable = false, length = 100)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private ClassmateStatus status;

}
