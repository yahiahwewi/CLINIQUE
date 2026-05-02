package com.example.demo.repository;

import com.example.demo.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByOrderByCreatedAtDesc();

    @Query("""
            select a from Announcement a
             where a.active = true
               and (a.expiresAt is null or a.expiresAt > :now)
               and (a.audience = :audienceA or a.audience = :audienceB)
            order by a.createdAt desc
            """)
    List<Announcement> findActiveForAudience(
            @Param("now") LocalDateTime now,
            @Param("audienceA") Announcement.Audience audienceA,
            @Param("audienceB") Announcement.Audience audienceB
    );
}
