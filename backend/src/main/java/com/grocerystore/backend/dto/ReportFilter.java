package com.grocerystore.backend.dto;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import com.grocerystore.backend.exception.BadRequestException;

public record ReportFilter(
        LocalDate from,
        LocalDate to,
        Integer storeId,
        Integer regionId) {

    public ReportFilter {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be on or before to");
        }
    }

    public static ReportFilter none() {
        return new ReportFilter(null, null, null, null);
    }

    public void appendTo(StringBuilder sql, List<Object> args) {
        if (from != null) {
            sql.append(" AND o.order_date >= ?");
            args.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND o.order_date <= ?");
            args.add(Date.valueOf(to));
        }
        if (storeId != null) {
            sql.append(" AND o.store_id = ?");
            args.add(storeId);
        }
        if (regionId != null) {
            sql.append(" AND s.region_id = ?");
            args.add(regionId);
        }
    }
}
