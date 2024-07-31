package com.nomz.doctorstudy.conference.repository;

import com.nomz.doctorstudy.conference.entity.Conference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConferenceRepository extends JpaRepository<Conference, Long> {
}
