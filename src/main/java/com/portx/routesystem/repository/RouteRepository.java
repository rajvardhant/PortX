package com.portx.routesystem.repository;

import com.portx.routesystem.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findTop10ByOrderByCreatedAtDesc();
}
