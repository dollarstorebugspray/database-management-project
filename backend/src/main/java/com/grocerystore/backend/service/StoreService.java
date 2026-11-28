package com.grocerystore.backend.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.RegionRequest;
import com.grocerystore.backend.dto.RegionResponse;
import com.grocerystore.backend.dto.StoreRequest;
import com.grocerystore.backend.dto.StoreResponse;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.util.Ids;
import com.grocerystore.backend.util.Text;

@Service
public class StoreService {

    private static final String SELECT_STORES = """
            SELECT s.store_id, s.street, s.city, s.state, s.zip_code, s.manager,
                   s.region_id, r.region_name,
                   (SELECT COUNT(*) FROM Salesperson e WHERE e.store_id = s.store_id) AS salesperson_count
            FROM Store s
            JOIN Region r ON r.region_id = s.region_id
            """;

    private static final String SELECT_REGIONS = """
            SELECT r.region_id, r.region_name, r.region_manager,
                   (SELECT COUNT(*) FROM Store s WHERE s.region_id = r.region_id) AS store_count
            FROM Region r
            """;

    private final JdbcTemplate jdbcTemplate;

    public StoreService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> findStores() {
        return jdbcTemplate.query(SELECT_STORES + " ORDER BY s.store_id", this::mapStore);
    }

    @Transactional(readOnly = true)
    public StoreResponse findStore(Integer storeId) {
        List<StoreResponse> stores = jdbcTemplate.query(
                SELECT_STORES + " WHERE s.store_id = ?",
                this::mapStore,
                storeId);
        if (stores.isEmpty()) {
            throw new ResourceNotFoundException("Store " + storeId + " was not found");
        }
        return stores.getFirst();
    }

    @Transactional
    public StoreResponse createStore(StoreRequest request) {
        findRegion(request.regionId());
        int storeId = Ids.next(jdbcTemplate, "Store", "store_id");
        jdbcTemplate.update("""
                INSERT INTO Store (store_id, street, city, state, zip_code, manager, region_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                storeId,
                request.street().trim(),
                request.city().trim(),
                request.state().trim().toUpperCase(),
                request.zipCode().trim(),
                Text.trimToNull(request.manager()),
                request.regionId());
        return findStore(storeId);
    }

    @Transactional
    public StoreResponse updateStore(Integer storeId, StoreRequest request) {
        findStore(storeId);
        findRegion(request.regionId());
        jdbcTemplate.update("""
                UPDATE Store
                SET street = ?, city = ?, state = ?, zip_code = ?, manager = ?, region_id = ?
                WHERE store_id = ?
                """,
                request.street().trim(),
                request.city().trim(),
                request.state().trim().toUpperCase(),
                request.zipCode().trim(),
                Text.trimToNull(request.manager()),
                request.regionId(),
                storeId);
        return findStore(storeId);
    }

    @Transactional(readOnly = true)
    public List<RegionResponse> findRegions() {
        return jdbcTemplate.query(SELECT_REGIONS + " ORDER BY r.region_id", this::mapRegion);
    }

    @Transactional(readOnly = true)
    public RegionResponse findRegion(Integer regionId) {
        List<RegionResponse> regions = jdbcTemplate.query(
                SELECT_REGIONS + " WHERE r.region_id = ?",
                this::mapRegion,
                regionId);
        if (regions.isEmpty()) {
            throw new ResourceNotFoundException("Region " + regionId + " was not found");
        }
        return regions.getFirst();
    }

    @Transactional
    public RegionResponse createRegion(RegionRequest request) {
        requireUniqueRegionName(request.regionName(), null);
        int regionId = Ids.next(jdbcTemplate, "Region", "region_id");
        jdbcTemplate.update(
                "INSERT INTO Region (region_id, region_name, region_manager) VALUES (?, ?, ?)",
                regionId,
                request.regionName().trim(),
                Text.trimToNull(request.regionManager()));
        return findRegion(regionId);
    }

    @Transactional
    public RegionResponse updateRegion(Integer regionId, RegionRequest request) {
        findRegion(regionId);
        requireUniqueRegionName(request.regionName(), regionId);
        jdbcTemplate.update(
                "UPDATE Region SET region_name = ?, region_manager = ? WHERE region_id = ?",
                request.regionName().trim(),
                Text.trimToNull(request.regionManager()),
                regionId);
        return findRegion(regionId);
    }

    private void requireUniqueRegionName(String name, Integer excludeRegionId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM Region
                WHERE LOWER(region_name) = LOWER(?) AND region_id <> ?
                """,
                Integer.class,
                name.trim(),
                excludeRegionId == null ? 0 : excludeRegionId);
        if (count != null && count > 0) {
            throw new BadRequestException("A region named " + name.trim() + " already exists");
        }
    }

    private StoreResponse mapStore(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StoreResponse(
                resultSet.getInt("store_id"),
                resultSet.getString("street"),
                resultSet.getString("city"),
                resultSet.getString("state"),
                resultSet.getString("zip_code"),
                resultSet.getString("manager"),
                resultSet.getInt("region_id"),
                resultSet.getString("region_name"),
                resultSet.getInt("salesperson_count"));
    }

    private RegionResponse mapRegion(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RegionResponse(
                resultSet.getInt("region_id"),
                resultSet.getString("region_name"),
                resultSet.getString("region_manager"),
                resultSet.getInt("store_count"));
    }
}
