package com.teleapps.schedulemate.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teleapps.schedulemate.domain.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {


}
