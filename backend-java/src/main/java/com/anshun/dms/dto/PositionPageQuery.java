package com.anshun.dms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PositionPageQuery {
    @Min(value = 1, message = "页码必须大于 0") private int page = 1;
    @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 1000, message = "每页数量不能超过 1000") private int limit = 10;
    private String district;
    private String status;
    private String search;
    public int getPage() { return page; } public void setPage(int page) { this.page = page; }
    public int getLimit() { return limit; } public void setLimit(int limit) { this.limit = limit; }
    public String getDistrict() { return district; } public void setDistrict(String district) { this.district = district; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getSearch() { return search; } public void setSearch(String search) { this.search = search; }
    public int getOffset() { return (page - 1) * limit; }
}
