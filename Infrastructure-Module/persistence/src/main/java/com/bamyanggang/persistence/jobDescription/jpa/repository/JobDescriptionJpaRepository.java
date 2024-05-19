package com.bamyanggang.persistence.jobDescription.jpa.repository;

import com.bamyanggang.domainmodule.domain.jobDescription.enums.WriteStatus;
import com.bamyanggang.persistence.jobDescription.jpa.entity.JobDescriptionJpaEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobDescriptionJpaRepository extends JpaRepository<JobDescriptionJpaEntity, UUID> {

    @Query("SELECT j FROM JobDescriptionJpaEntity j WHERE j.userId = :userId AND (:writeStatus is null or j.writeStatus = :writeStatus)")
    Page<JobDescriptionJpaEntity> findAllByUserIdAndWriteStatus(@Param("userId") UUID userId, @Param("writeStatus") WriteStatus writeStatus, @Param("date")  Pageable pageable);

    @Query("SELECT j FROM JobDescriptionJpaEntity j WHERE j.userId = :userId AND (:writeStatus is null or j.writeStatus = :writeStatus) AND j.endedAt > :date ORDER BY j.endedAt ASC")
    Page<JobDescriptionJpaEntity> findAllByUserIdAndWriteStatusAndTime(@Param("userId") UUID userId, @Param("writeStatus") WriteStatus writeStatus,
                                                                       @Param("date") LocalDateTime now, Pageable pageable);

}
