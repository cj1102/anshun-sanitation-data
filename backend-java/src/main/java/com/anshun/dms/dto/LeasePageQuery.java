package com.anshun.dms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class LeasePageQuery {
    @Min(value = 1, message = "页码必须大于 0") private int page = 1;
    @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 1000, message = "每页数量不能超过 1000") private int limit = 10;
    private String search;
    @JsonProperty("lessee_company") private String lesseeCompany;
    @JsonProperty("approval_status") private String approvalStatus;
    public int getPage() { return page; } public void setPage(int page) { this.page = page; }
    public int getLimit() { return limit; } public void setLimit(int limit) { this.limit = limit; }
    public String getSearch() { return search; } public void setSearch(String search) { this.search = search; }
    public String getLesseeCompany() { return lesseeCompany; } public void setLesseeCompany(String lesseeCompany) { this.lesseeCompany = lesseeCompany; }
    public String getApprovalStatus() { return approvalStatus; } public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public void setLessee_company(String lesseeCompany) { this.lesseeCompany = lesseeCompany; }
    public void setApproval_status(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public int getOffset() { return (page - 1) * limit; }
}
