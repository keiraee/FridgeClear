package com.sccothe.fridgeclear.telemetry.repository;

import com.sccothe.fridgeclear.telemetry.domain.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    Page<AccessLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT al.clientId) FROM AccessLog al WHERE al.createdAt >= :since")
    long countDistinctClientIdSince(@Param("since") LocalDateTime since);

    @Query(value = """
            SELECT device_type AS label, COUNT(*) AS cnt
            FROM access_log
            WHERE device_type IS NOT NULL AND device_type <> ''
            GROUP BY device_type
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> countGroupByDeviceType();

    @Query(value = """
            SELECT access_type AS label, COUNT(*) AS cnt
            FROM access_log
            WHERE access_type IS NOT NULL AND access_type <> ''
            GROUP BY access_type
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> countGroupByAccessType();

    @Query(value = """
            SELECT gps_status AS label, COUNT(*) AS cnt
            FROM access_log
            WHERE gps_status IS NOT NULL AND gps_status <> ''
            GROUP BY gps_status
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> countGroupByGpsStatus();

    @Query(value = """
            SELECT page_path AS label, COUNT(*) AS cnt
            FROM access_log
            WHERE page_path IS NOT NULL AND page_path <> ''
            GROUP BY page_path
            ORDER BY cnt DESC
            LIMIT 8
            """, nativeQuery = true)
    List<Object[]> countTopPages();

    @Query(value = """
            SELECT DATE(created_at) AS day, COUNT(*) AS cnt
            FROM access_log
            WHERE created_at >= :since
            GROUP BY DATE(created_at)
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> countDailySince(@Param("since") LocalDateTime since);
}
